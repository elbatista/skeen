package skeen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;
import base.Node;
import skeen.messages.SkeenMessage;
import skeen.messages.SkeenMessage.Type;
import skeen.proxies.SkeenClientProxy;
import util.ArgsParser;
import util.FileManager;
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
    protected int [][][][] wloadDist4dests;
    protected int [][][][][] wloadDist5dests;

    public SkeenClient(short id, ArgsParser args, boolean start){
        super(id);
        this.args = args;
        totalTime = args.getDuration();
        this.files = new FileManager();
        this.localityPercentage = args.getLocality();
        ArrayList<Node> nodes = files.loadHosts();
        syncAllConnections = new CyclicBarrier(nodes.size()+1);
        for(Node server : nodes) connectTo(server, syncAllConnections);
        numNodes = (short) nodes.size();
        destsSizes = new int [numNodes];
        wloadDist2dests = new int [numNodes][numNodes];
        wloadDist3dests = new int [numNodes][numNodes][numNodes];
        wloadDist4dests = new int [numNodes][numNodes][numNodes][numNodes];
        wloadDist5dests = new int [numNodes][numNodes][numNodes][numNodes][numNodes];
        
        if(start) start();
    }
    
    // generates an unique message id, based on the client id
    protected int nextSeqNumber(){
        seqNumber++;
        while((seqNumber % args.getClientCount()) != getId())
            seqNumber++;
        return seqNumber;
    }

    private void start() {
        // wait all netty threads connect to all servers
        try {syncAllConnections.await();} catch(InterruptedException|BrokenBarrierException e){print("Broken barrier!!!!");}
        // send initialization message to all servers
        sendInitMessage();
        sleep(1000);
        // send ready message to a server
        // the server will reply when all clients are ready, then we "guarantee" all clients start at (~) the same time
        sendReadyMessage();
        print("All other clients ready!");
        
        print("Started skeen experiment");
        print("Locality:", args.getLocality(), "%");

        stats = new Stats(totalTime);
        long startTime = System.nanoTime(), now;
        long elapsed = 0, usLat = startTime;

        while ((elapsed / 1e9) < totalTime) {
            SkeenMessage m = newMessage();
            multicast(m);
            now = System.nanoTime();
            stats.store((now - usLat) / 1000, (m.getDst().length > 1));
            elapsed = (now - startTime);

            destsSizes[m.getDst().length-1]++;
            computeDistribution(m);
            usLat = now;
        }

        if (stats.getCount() > 0) {
            try {Files.createDirectories(Paths.get("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion())));} catch (IOException e) {}
            stats.persist("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion()) + "/" + getId() + "-stats-client-skeen.txt", 15);
            print("LOCAL STATS:", stats);
        }

        sendEndMessage();

        for(int i = 0; i < destsSizes.length; i++) print("# of msgs to", i+1, "dests:", destsSizes[i]);

        printWloadDistribution();       

        print("Finished skeen experiment. Elapsed: ", elapsed / 1e9, "seconds");
        exit();
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
        
        print("Wload for destination size 4:");
        for(int i = 0; i < numNodes; i++)
            for(int j = 0; j < numNodes; j++)
                for(int k = 0; k < numNodes; k++)
                    for(int l = 0; l < numNodes; l++)
                        if(wloadDist4dests[i][j][k][l] > 0) print("# of msgs to [",i, j, k, l, "]:", wloadDist4dests[i][j][k][l]);
        
        print("Wload for destination size 5:");
        for(int i = 0; i < numNodes; i++)
            for(int j = 0; j < numNodes; j++)
                for(int k = 0; k < numNodes; k++)
                    for(int l = 0; l < numNodes; l++)
                        for(int m = 0; m < numNodes; m++)
                            if(wloadDist5dests[i][j][k][l][m] > 0) print("# of msgs to [",i, j, k, l, m, "]:", wloadDist5dests[i][j][k][l][m]);
    }

    protected void computeDistribution(SkeenMessage m) {
        if (m.getDst().length == 2){
            wloadDist2dests[m.getDst()[0]][m.getDst()[1]]++;
        } else if (m.getDst().length == 3){
            wloadDist3dests[m.getDst()[0]][m.getDst()[1]][m.getDst()[2]]++;
        } else if (m.getDst().length == 4) {
            wloadDist4dests[m.getDst()[0]][m.getDst()[1]][m.getDst()[2]][m.getDst()[3]]++;
        } else if (m.getDst().length == 5) {
            wloadDist5dests[m.getDst()[0]][m.getDst()[1]][m.getDst()[2]][m.getDst()[3]][m.getDst()[4]]++;
        }
    }

    protected SkeenMessage newMessageTo(short... dst){
        SkeenMessage m = newMessage();
        m.setDst(dst);
        return m;
    }

    private SkeenMessage newMessage(){
        SkeenMessage m = new SkeenMessage(nextSeqNumber());
        m.setType(Type.MSG);
        m.setDst(randomDests());
        m.setCliId(getId());
        return m;
    }

    private short[] randomDests(){
        Random r = new Random();
        if(args.getNumPartitions() > 1){
            if(localityPercentage > 0){
                if(args.getNumPartitions() == 2)
                    return run2ShardsWithLocality(r, numNodes);

                print("Bad config shards vs locality");
                exit();
                return null;
            }

            short [] tempdst = new short[args.getNumPartitions()];
            // 1
            tempdst[0] = (short)r.nextInt(numNodes);

            // 2
            do {tempdst[1] = (short)r.nextInt(numNodes);}
            while(tempdst[0] == tempdst[1]);

            // 3
            if(args.getNumPartitions() >= 3){
                do {tempdst[2] = (short)r.nextInt(numNodes);}
                while(tempdst[0] == tempdst[2] || tempdst[1] == tempdst[2]);
            }

            // 4
            if(args.getNumPartitions() >= 4){
                do {tempdst[3] = (short)r.nextInt(numNodes);}
                while(tempdst[0] == tempdst[3] || tempdst[1] == tempdst[3] || tempdst[2] == tempdst[3]);
            }

            // 5
            if(args.getNumPartitions() >= 5){
                do {tempdst[4] = (short)r.nextInt(numNodes);}
                while(tempdst[0] == tempdst[4] || tempdst[1] == tempdst[4] || tempdst[2] == tempdst[4] || tempdst[3] == tempdst[4]);
            }

            // 6
            if(args.getNumPartitions() == 6){
                if(numNodes != 6) {
                    print("Bad config shards vs nodes");
                    exit();
                }
                tempdst[0] = (short) 0;
                tempdst[1] = (short) 1;
                tempdst[2] = (short) 2;
                tempdst[3] = (short) 3;
                tempdst[4] = (short) 4;
                tempdst[5] = (short) 5;
                return tempdst;
            }
            
            Arrays.sort(tempdst);
            return tempdst;
        }

        Set<Short> uniqueNumbers = new HashSet<>();
        int size = r.nextInt(numNodes)+1;
        while (uniqueNumbers.size() < size)
            uniqueNumbers.add((short)r.nextInt(numNodes));
        short [] tempdst = new short[size];
        short i = 0;
        for(short u : uniqueNumbers.stream().sorted().collect(Collectors.toList())){
            tempdst[i] = u;
            i++;
        }
        return tempdst;
    }

    private short[] run2ShardsWithLocality(Random r, short numNodes) {
        short [] tempdst = new short[2];
        int rand = r.nextInt(100);
        int halfLocality = localityPercentage/2;

        if(numNodes == 3){
            if(rand < localityPercentage){
                if(halfLocality < rand){
                    tempdst[0] = 0;
                    tempdst[1] = 1;
                }
                else {
                    tempdst[0] = 1;
                    tempdst[1] = 2;
                }
            }
            else {
                tempdst[0] = 0;
                tempdst[1] = 2;
            }
            return tempdst;
        }

        if(numNodes == 6){
            if(rand < 20){
                tempdst[0] = 0;
                tempdst[1] = 1;
            } else if (rand >= 20 && rand < 40){
                tempdst[0] = 1;
                tempdst[1] = 2;
            } else if (rand >= 40 && rand < 60){
                tempdst[0] = 2;
                tempdst[1] = 3;
            } else if (rand >= 60 && rand < 80){
                tempdst[0] = 3;
                tempdst[1] = 4;
            } else if (rand >= 80 && rand <  96){
                tempdst[0] = 4;
                tempdst[1] = 5;
            } else if (rand >= 96 && rand <  97){
                tempdst[0] = 0;
                tempdst[1] = 2;
            } else if (rand >= 97 && rand <  98){
                tempdst[0] = 1;
                tempdst[1] = 3;
            } else if (rand >= 98 && rand <  99){
                tempdst[0] = 2;
                tempdst[1] = 4;
            } else if (rand >= 99){
                tempdst[0] = 3;
                tempdst[1] = 5;
            }
            return tempdst;
        }

        return tempdst;
    }

}