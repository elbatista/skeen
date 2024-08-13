package skeen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;
import base.Node;
// import skeen.messages.SkeenMessage;
import skeen.messages.SkeenMessage;
import skeen.messages.SkeenMessage.Type;
import skeen.proxies.SkeenClientProxy;
import util.ArgsParser;
import util.FileManager;
import util.OrderItem;
import util.Stats;

public class SkeenClient extends SkeenClientProxy {
    protected ArgsParser args;
    protected int seqNumber, totalTime;
    protected short numNodes = 0;
    protected CyclicBarrier syncAllConnections;
    protected FileManager files;
    private int localityPercentage = 0;
    private int [] destsSizes;
    protected int [][] wloadDist2dests;
    protected int [][][] wloadDist3dests;
    protected final Random gen;
    private HashMap<Short, String> nearestWHs = new HashMap<>();
    double AcumTt = 0;
    int TtCount = 0;
    protected final Random thinkTimeRand;
    private boolean sendPayload, tt, includeLocalMsgs;
    
    // Tpcc workload distribution
    private static final int newOrderWeight = 45;
    private static final int paymentWeight = 43;
    private static final int orderStatusWeight = 4;
    private static final int deliveryWeight = 4;
    private static final int stockLevelWeight = 4;

    public SkeenClient(short id, ArgsParser args, boolean start){
        super(id);
        this.args = args;
        totalTime = args.getDuration();
        this.files = new FileManager();
        this.sendPayload = args.shouldSendPayload();
        this.tt = args.thinkTime();
        this.includeLocalMsgs = args.includeLocalMsgs();
        this.localityPercentage = args.getLocality();
        ArrayList<Node> nodes = files.loadHosts();
        FileManager.loadLocalityFile(nearestWHs);
        syncAllConnections = new CyclicBarrier(nodes.size()+1);
        for(Node server : nodes) connectTo(server, syncAllConnections);
        numNodes = (short) nodes.size();
        warehouse = (short) args.getHomeWarehouse();
        destsSizes = new int [numNodes];
        wloadDist2dests = new int [numNodes][numNodes];
        wloadDist3dests = new int [numNodes][numNodes][numNodes];
        gen = new Random(System.nanoTime());
        thinkTimeRand = new Random(System.nanoTime());
        if(start) start();
    }
    
    // generates an unique message id, based on the client id
    private int nextSeqNumber(){
        seqNumber++;
        while((seqNumber % args.getClientCount()) != getId())
            seqNumber++;
        return seqNumber;
    }

    private void start() {
        // wait all netty threads connect to all servers
        try {syncAllConnections.await();} catch(InterruptedException|BrokenBarrierException e){print("Broken barrier!!!!");}
        printF("Connected to all servers!");
        sleep(10000);
        
        // send initialization message to all servers
        sendInitMessage();
        // send ready message to a server
        // the server will reply when all clients are ready, then we "guarantee" all clients start at (~) the same time
        sendReadyMessage();
        print("All other clients ready!");

        print("Started skeen experiment");
        if(args.getNumPartitions() > 0) print (args.getNumPartitions(), "partitions");
        printF("Locality:", localityPercentage, "%");
        printF("My home warehouse:", warehouse);
        if(args.getNumMessages() > 0) printF("Will send", args.getNumMessages(), "messages");
        if(sendPayload) printF("Sending a TPCC like PAYLOAD in messages");
        if(tt) printF("Using Think Time");
        if(includeLocalMsgs) printF("Including LOCAL messages");
        else printF("ONLY GLOBAL messages");
        stats = new Stats(totalTime, numNodes);
        long startTime = System.nanoTime();
        long now;
        long elapsed = 0;//, usLat = startTime;
        int totalMsgs=0;

        while ((elapsed / 1e9) < totalTime) {
            SkeenMessage m = newMessage();
            
            generatePayload(m);

            now = System.nanoTime();
            multicast(m);
            stats.store((System.nanoTime() - now) / 1000, (m.getDst().length > 1));

            elapsed = (now - startTime);

            destsSizes[m.getDst().length-1]++;

            computeDistribution(m);

            if(tt) thinkTime();
            
            // usLat = now;
            totalMsgs++;
            if(args.getNumMessages() > 0 && totalMsgs == args.getNumMessages()) break;
        }

        if (stats.getCount() > 0) {
            try {Files.createDirectories(Paths.get("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion())));} catch (IOException e) {}
            stats.persist("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion()) + "/" + getId() + "-stats-client-skeen.txt", 15);
            stats.persistPerNodes("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion()) + "/" + getId() + "-stats-client-skeen-per-node.txt", 15);
            print("LOCAL STATS:", stats);
        }

        sendEndMessage();

        for(int i = 0; i < destsSizes.length; i++) print("# of msgs to", i+1, "dests:", destsSizes[i]);

        printWloadDistribution();       

        print("Finished skeen experiment. Elapsed: ", elapsed / 1e9, "seconds");
        exit();
    }

    private void generatePayload(SkeenMessage m) {
        if(!sendPayload) {
            m.setTransaction(SkeenMessage.TransactionType.NOPAYLOAD);
            return;
        }
        int transactionType = randomNumber(1, 100, gen);
        m.setOrderDate(new Date());
        if (transactionType <= newOrderWeight) {
            m.setTransaction(SkeenMessage.TransactionType.NEW);
            int numItems = randomNumber(5, 15, gen);
            for (int i = 0; i < numItems; i++) {
                m.getItems().add(new OrderItem(randomNumber(1, 100000, gen), randomNumber(1, 10, gen)));
            }
        } else if (transactionType <= newOrderWeight + paymentWeight) {
            m.setTransaction(SkeenMessage.TransactionType.PAYMENT);
            m.setPaymentAmount(randomNumber(1, 5000, gen));
        } else if (transactionType <= newOrderWeight + paymentWeight + orderStatusWeight) {
            m.setTransaction(SkeenMessage.TransactionType.STATUS);
        } else if (transactionType <= newOrderWeight + paymentWeight + orderStatusWeight + deliveryWeight) {
            m.setTransaction(SkeenMessage.TransactionType.DELIVERY);
            m.setCarrierid_or_threshold(randomNumber(1, 10, gen));
        } else if (transactionType <= newOrderWeight + paymentWeight + orderStatusWeight + deliveryWeight + stockLevelWeight) {
            m.setTransaction(SkeenMessage.TransactionType.STOCK);
            m.setCarrierid_or_threshold(randomNumber(10, 20, gen));
        }
    }

    private void thinkTime() {
        /*
         * Tt = -log(r) * u 
         * where: log  = natural log (base e)  
         * Tt  = think time  
         * r  = random number uniformly distributed between 0 and 1  
         * u  = mean think time 
         * 
         * Each distribution may be truncated at 10 times its mean value
         */
        double r = thinkTimeRand.nextDouble();
        double u = 100;
        double Tt = -Math.log(r) * u;
        if(Tt > (1000)) Tt = 1000;
        sleep((long)Tt);
    }

    protected void printWloadDistribution() {
        print("Wload for destination size 2:");
        for(int i = 0; i < numNodes; i++)
            for(int j = 0; j < numNodes; j++)
                if(wloadDist2dests[i][j] > 0) print("# of msgs to [",i, j, "]:", wloadDist2dests[i][j]);

        print("Wload for destination size 3:");
        for(int i = 0; i < numNodes; i++)
            for(int j = 0; j < numNodes; j++)
                for(int k = 0; k < numNodes; k++)
                    if(wloadDist3dests[i][j][k] > 0) print("# of msgs to [",i, j, k, "]:", wloadDist3dests[i][j][k]);
    }

    protected void computeDistribution(SkeenMessage m) {
        if (m.getDst().length == 2){
            wloadDist2dests[m.getDst()[0]][m.getDst()[1]]++;
        } else if (m.getDst().length == 3){
            wloadDist3dests[m.getDst()[0]][m.getDst()[1]][m.getDst()[2]]++;
        }
    }

    protected SkeenMessage newMessageTo(short... dst){
        SkeenMessage m = new SkeenMessage(nextSeqNumber());
        m.setType(Type.MSG);
        m.setCliId(getId());
        m.setDst(dst);
        return m;
    }

    private SkeenMessage newMessage(){
        SkeenMessage m = new SkeenMessage(nextSeqNumber());
        m.setType(Type.MSG);
        m.setDst(generateDests());
        m.setCliId(getId());
        return m;
    }

    private short[] generateDests(){

        if(includeLocalMsgs && randomNumber(1, 100, gen) <= 90){
            return new short[]{warehouse};
        }

        if(localityPercentage == 0){
            return generateRandDests();
        }
        if(randomNumber(1, 100, gen) <= localityPercentage) 
            return generate2Dests();
        return generate3Dests();
    }
    private short[] generateRandDests() {
        Set<Short> uniqueNumbers = new HashSet<>();
        int size = randomNumber(2, numNodes, gen); // only global
        while (uniqueNumbers.size() < size)
            uniqueNumbers.add((short)randomNumber(0, numNodes-1, gen));
        short [] tempdst = new short[size];
        short i = 0;
        for(short u : uniqueNumbers.stream().sorted().collect(Collectors.toList())){
            tempdst[i] = u;
            i++;
        }
        return tempdst;
    }

    private short[] generate2Dests(){
        short [] tempdst = new short[2];
        tempdst[0] = warehouse;

        if(randomNumber(1, 100, gen) <= localityPercentage)
            tempdst[1] = getNearestWH(0);
        else 
            tempdst[1] = getNearestWH(1);

        Arrays.sort(tempdst);

        return tempdst;
    }

    private short[] generate3Dests(){
        short [] tempdst = new short[3];
        tempdst[0] = warehouse;
        if(randomNumber(1, 100, gen) <= localityPercentage){
            tempdst[1] = getNearestWH(0);
            tempdst[2] = getNearestWH(1);
        }else {
            tempdst[1] = getNearestWH(1);
            tempdst[2] = getNearestWH(2);
        }
        LinkedHashSet<Short> set = new LinkedHashSet<Short>();
 
        // remove duplicates
        for (short s : tempdst) set.add(s);
        short [] finaldst = new short[set.size()];
        int i = 0;
        for(short s : set){
            finaldst[i] = s;
            i++;
        }
        Arrays.sort(finaldst);

        return finaldst;
    }

    private short getNearestWH(int index) {
        short tempdst = -1;

        try{tempdst = Short.valueOf(nearestWHs.get((short)warehouse).split(" ")[index].trim());} catch(Exception e){}

        if(tempdst == -1){
            // simply get the next HW in order of id
            tempdst = (short)(warehouse+1);
            if(tempdst == numNodes) tempdst = (short)(warehouse-1);
        }
        return tempdst;
    }

    private short getNearestWH(short warehouseparam) {
        if(numNodes == 9){
            switch(warehouseparam){
                case 0: return 1;
                case 1: return 2;
                case 2: return 1;
                case 3: return 4;
                case 4: return 5;
                case 5: return 4;
                case 6: return 7;
                case 7: return 8;
                case 8: return 7;
                default: return warehouseparam;
            }
        }
        else if(numNodes == 12){
            switch(warehouseparam){
                case 0: return 1;
                case 1: return 2;
                case 2: return 1;
                case 3: return 2;
                case 4: return 5;
                case 5: return 6;
                case 6: return 7;
                case 7: return 6;
                case 8: return 9;
                case 9: return 10;
                case 10: return 9;
                case 11: return 10;
                default: return warehouseparam;
            }
        } 
        return warehouseparam;
    }
    private short getSecondNearestWH(short warehouseparam) {
        if(numNodes == 9){
            switch(warehouseparam){
                case 0: return 2;
                case 1: return 3;
                case 2: return 2;
                case 3: return 5;
                case 4: return 2;
                case 5: return 3;
                case 6: return 8;
                case 7: return 5;
                case 8: return 6;
                default: return warehouseparam;
            }
        }
        else if(numNodes == 12){
            switch(warehouseparam){
                case 0: return 2;
                case 1: return 3;
                case 2: return 0;
                case 3: return 1;
                case 4: return 6;
                case 5: return 7;
                case 6: return 4;
                case 7: return 5;
                case 8: return 10;
                case 9: return 11;
                case 10: return 8;
                case 11: return 9;
                default: return warehouseparam;
            }
        } 
        return warehouseparam;
    }
    private short getThirdNearestWH(short warehouseparam) {
        if(numNodes == 9){
            switch(warehouseparam){
                case 0: return 3;
                case 1: return 4;
                case 2: return 5;
                case 3: return 0;
                case 4: return 7;
                case 5: return 8;
                case 6: return 3;
                case 7: return 4;
                case 8: return 5;
                default: return warehouseparam;
            }
        }
        else if(numNodes == 12){
            switch(warehouseparam){
                case 0: return 3;
                case 1: return 4;
                case 2: return 5;
                case 3: return 0;
                case 4: return 7;
                case 5: return 8;
                case 6: return 2;
                case 7: return 4;
                case 8: return 11;
                case 9: return 6;
                case 10: return 7;
                case 11: return 8;
                default: return warehouseparam;
            }
        } 
        return warehouseparam;
    }

    public static int randomNumber(int min, int max, Random r) {
        return (int) (r.nextDouble() * (max - min + 1) + min);
    }

}