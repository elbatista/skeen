package skeen.proxies;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import io.netty.channel.Channel;
import skeen.comms.SkeenNettyServerChannel;
import skeen.messages.SkeenMessage;
import skeen.messages.SkeenMessage.Type;
import util.FileManager;

public abstract class SkeenServerProxy extends SkeenClientProxy {
    protected ConcurrentLinkedQueue<SkeenMessage> bufferQueue;
    private HashMap<Integer, Channel> cliChannels;
    protected int numCliEndsRecv = 0, numCliReadyRecv = 0, numClients = 0, localMsgs = 0;
    public SkeenServerProxy(short id, int numClients){
        super(id);
        this.numClients = numClients;
        bufferQueue = new ConcurrentLinkedQueue<>();
        cliChannels = new HashMap<>();
        new SkeenNettyServerChannel(this, this);
        new Thread(new Runnable() {
            public void run(){
                while(true) {
                    SkeenMessage m = bufferQueue.poll();
                    if(m != null) receive(m);
                }
            }
        }).start();
    }

    public void buffer(SkeenMessage m){
        if(m.getType() == Type.MSG && m.getDst().length == 1){
            localMsgs++;
            sendReply(m);
            return;
        }
        bufferQueue.offer(m);
    }

    private void receive(SkeenMessage m) {
        switch(m.getType()){
            case MSG: receiveMsg(m); break;
            case STEP1: receiveStep1Msg(m); break;
            case STEP2: receiveStep2Msg(m); break;
            // message used only to establish a connection to each client
            case CONN: {
                cliChannels.put(m.getCliId(), m.getChannelIn());
                m.setSender(getId());
                m.getChannelIn().writeAndFlush(m);
                print("Channel to client", m.getCliId(), ":", m.getChannelIn());
                break;
            }
            // message used only to ensure all clients are ready (connected) before all clients start multicasting
            case READY: receiveReady(m); break;
            // message used only to end a connection to a client
            case END: receiveEnd(m); break;
            default: {
                print("Should never reach here !");
                new FileManager().stop();
                exit();
            }
        }
    }
    
    private void receiveMsg(SkeenMessage m) {
        m.setType(Type.STEP1);
        for(short dst : m.getDst()){
            if(dst != getId())
                send(m, dst);
        }
        SkeenMessage aux = new SkeenMessage(m.getId());
        aux.setType(m.getType());
        aux.setSender(m.getSender());
        aux.setTimestamp(m.getTimestamp());
        aux.setCliId(m.getCliId());
        aux.setDst(m.getDst());
        receiveStep1Msg(aux);
    }

    protected void sendReply(SkeenMessage m){
        SkeenMessage reply = new SkeenMessage(m.getId());
        reply.setSender(getId());
        reply.setType(Type.REPLY);
        cliChannels.get(m.getCliId()).writeAndFlush(reply);
    }

    private void receiveReady(SkeenMessage m) {
        numCliReadyRecv++;
        if(numCliReadyRecv == numClients){
            print("All", numClients, " clients are ready. They will start multicasting...");
            for(int i = 0; i < numClients; i++){
                // reply to all clients
                m.setSender(getId());
                cliChannels.get(i).writeAndFlush(m);
            }
        }
    }

    private void receiveEnd(SkeenMessage m) {
        numCliEndsRecv++;
        m.setSender(getId());
        m.getChannelIn().writeAndFlush(m);
        if(numCliEndsRecv == numClients){
            print("All", numClients, " clients done!");
            finish();
        }
    }
    protected abstract void finish();
    protected abstract void receiveStep1Msg(SkeenMessage m);
    protected abstract void receiveStep2Msg(SkeenMessage m);
}