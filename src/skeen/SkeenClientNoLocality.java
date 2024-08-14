package skeen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;
import base.Node;
// import skeen.messages.SkeenMessage;
import skeen.messages.SkeenMessage;
import skeen.messages.SkeenMessage.TransactionType;
import skeen.messages.SkeenMessage.Type;
import skeen.proxies.SkeenClientProxy;
import util.ArgsParser;
import util.FileManager;
import util.Stats;

public class SkeenClientNoLocality extends SkeenClientProxy {
    protected ArgsParser args;
    protected int seqNumber, totalTime, numDests=1;
    protected short numNodes = 0;
    protected CyclicBarrier syncAllConnections;
    protected FileManager files;
    protected final Random gen;
    
    public SkeenClientNoLocality(short id, ArgsParser args, boolean start){
        super(id);
        this.args = args;
        totalTime = args.getDuration();
        this.files = new FileManager();
        ArrayList<Node> nodes = files.loadHosts();
        syncAllConnections = new CyclicBarrier(nodes.size()+1);
        for(Node server : nodes) connectTo(server, syncAllConnections);
        numNodes = (short) nodes.size();
        gen = new Random(System.nanoTime());
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
        print("Starting Skeen no Locality experiment");
        printF("Connected to all servers!");
        // send initialization message to all servers
        sendInitMessage();
        // send ready message to a server
        // the server will reply when all clients are ready, then we "guarantee" all clients start at (~) the same time
        sendReadyMessage();
        print("All other clients ready!");

        if(args.getNumPartitions() > 0) {
            print ("Sending messages to", args.getNumPartitions(), "destinations");
            numDests = args.getNumPartitions();
        }

        stats = new Stats(totalTime, numNodes);
        long startTime = System.nanoTime();
        long now;
        long elapsed = 0;

        while ((elapsed / 1e9) < totalTime) {
            SkeenMessage m = newMessage();
            
            now = System.nanoTime();
            
            multicast(m);

            stats.store((System.nanoTime() - now) / 1000, (m.getDst().length > 1));

            elapsed = (now - startTime);
            
        }

        if (stats.getCount() > 0) {
            try {Files.createDirectories(Paths.get("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion())));} catch (IOException e) {}
            stats.persist("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion()) + "/" + getId() + "-stats-client-skeen.txt", 15);
            stats.persistPerNodes("results" + (args.getRegion().equals("") ? "" : "/"+args.getRegion()) + "/" + getId() + "-stats-client-skeen-per-node.txt", 15);
            print("LOCAL STATS:", stats);
        }

        sendEndMessage();

        print("Finished skeen experiment. Elapsed: ", elapsed / 1e9, "seconds");
        exit();
    }


    protected SkeenMessage newMessageTo(short... dst){
        SkeenMessage m = new SkeenMessage(nextSeqNumber());
        m.setType(Type.MSG);
        m.setTransaction(TransactionType.NOPAYLOAD);
        m.setCliId(getId());
        m.setDst(dst);
        return m;
    }

    private SkeenMessage newMessage(){
        SkeenMessage m = new SkeenMessage(nextSeqNumber());
        m.setType(Type.MSG);
        m.setTransaction(TransactionType.NOPAYLOAD);
        m.setDst(generateRandDests());
        m.setCliId(getId());
        return m;
    }

    private short[] generateRandDests() {
        Set<Short> uniqueNumbers = new HashSet<>();

        int size = numDests; //randomNumber(2, numNodes, gen); // only global

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

    public static int randomNumber(int min, int max, Random r) {
        return (int) (r.nextDouble() * (max - min + 1) + min);
    }

}