package skeen.proxies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import base.Node;
import io.netty.channel.Channel;
import skeen.comms.SkeenNettyClientChannel;
import skeen.messages.SkeenMessage;
import skeen.messages.SkeenMessage.Type;
import util.Stats;

public class SkeenClientProxy extends Node {
    private HashMap<Short, Channel> outChannels;
    private Semaphore sema = new Semaphore(0);
    private ReentrantLock lock = new ReentrantLock();
    private ArrayList<SkeenMessage> replies = new ArrayList<>();
    private short expectedReplies = 0;
    protected Stats stats;
    protected short warehouse;
    private long startTime;
    private HashMap<Short, Long> latsPerNode = new HashMap<>();
    short lca;
    short[] dsts;

    public SkeenClientProxy(short id){
        super(id);
        outChannels = new HashMap<>();
    }

    public void connectTo(Node dest){
        new SkeenNettyClientChannel(dest, this);
    }

    public void connectTo(Node dest, CyclicBarrier syncAllConnections){
        new SkeenNettyClientChannel(dest, this, syncAllConnections);
    }

    public void setChannelToDest(Channel c, short dst){
        print("Channel to node", dst, ":", c);
        try {
            outChannels.put(dst, c);
        }
        catch(Exception e){
            e.printStackTrace();
            print(e);
            exit();
        }
    }

    public void sendInitMessage(){
        SkeenMessage m = new SkeenMessage(-1);
        m.setType(Type.CONN);
        m.setCliId(getId());
        for(short i : outChannels.keySet()){
            try {
                outChannels.get(i).writeAndFlush(m);
                sema.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendReadyMessage(){
        SkeenMessage m = new SkeenMessage();
        m.setType(Type.READY);
        m.setCliId(getId());
        try {
            outChannels.get((short)0).writeAndFlush(m);
            sema.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendEndMessage(){
        SkeenMessage m = new SkeenMessage();
        m.setType(Type.END);
        m.setCliId(getId());
        for(short i : outChannels.keySet()){
            try {
                outChannels.get(i).writeAndFlush(m);
                sema.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void receiveReplyInitMsg(SkeenMessage reply){
        print("Init OK - Server", reply.getSender());
        sema.release();
    }

    public void send(SkeenMessage m, short dst){
        try {
            outChannels.get(dst).writeAndFlush(m);
        }
        catch(Exception e){
            e.printStackTrace();
            print(e);
            exit();
        }
    }

    public void receiveReplyReadyMsg(SkeenMessage reply){
        print("Ready OK - Server", reply.getSender());
        sema.release();
    }

    public void receiveReplyEndMsg(SkeenMessage reply){
        print("End OK - Server", reply.getSender());
        sema.release();
    }

    public void receiveReply(SkeenMessage reply){
        lock.lock();

        // armazena latencia por nodo em microsegundo
        latsPerNode.put(reply.getSender(), ((System.nanoTime() - startTime) / 1000));


        replies.add(reply);
        if(replies.size() == expectedReplies){
            if(stats != null) stats.store(latsPerNode, expectedReplies>1, dsts, null);
            sema.release();
        }
        lock.unlock();
    }

    public SkeenMessage multicast(SkeenMessage m, short warehouse){
        replies.clear();
        expectedReplies = (short) m.getDst().length;
        send(m, warehouse);
        try {
            sema.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return replies.get(0);
    }

    public SkeenMessage multicast(SkeenMessage m){
        replies.clear();
        expectedReplies = (short) m.getDst().length;

        latsPerNode.clear();
        startTime = System.nanoTime();
        dsts = m.getDst();

        send(m, m.getDst()[0]);

        try {
            sema.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return replies.get(0);
    }
}