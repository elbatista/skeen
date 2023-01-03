package skeen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import util.ArgsParser;
import util.FileManager;
import java.util.TreeSet;
import base.Host;
import base.Node;
import skeen.messages.SkeenMessage;
import skeen.messages.SkeenMessage.Type;
import skeen.proxies.SkeenServerProxy;

public class SkeenNode extends SkeenServerProxy {
    protected int LC = 0, numNodes, step1, step2;
    protected TreeSet<SkeenMessage> ordered;
    protected ArrayList<SkeenMessage> pending;
    protected FileManager files;
    private HashMap<Integer, ArrayList<SkeenMessage>> receivedTimestamps;
    private ArrayList<SkeenMessage> history = new ArrayList<>();
    
    public SkeenNode(short id, ArgsParser args){
        super(id, args.getClientCount());
        LC = id;
        List<Node> nodes = new FileManager().loadHosts();
        numNodes = nodes.size();
        Host thisHost = null;
        for(Node n : nodes){
            if(n.getId() == id){
                thisHost = n.getHost();
                break;
            }
        }
        setHost(thisHost);
        print(this, "Skeen - Start listening ...", "Ini TS:", LC);
        this.ordered = new TreeSet<>();
        this.pending = new ArrayList<>();
        this.receivedTimestamps = new HashMap<>();
        this.files = new FileManager();
        // sets connection to all nodes
        for(Node node : nodes)
            connectTo(node);
    }

    @Override
    protected void receiveStep1Msg(SkeenMessage m){
        step1++;
        LC += numNodes;
        m.setTimestamp(LC);
        m.setType(Type.STEP2);
        m.setSender(getId());

        for(short dst : m.getDst()){
            if(dst != getId())
                send(m, dst);
        }

        pending.add(m);

        SkeenMessage aux = new SkeenMessage(m.getId());
        aux.setType(m.getType());
        aux.setSender(m.getSender());
        aux.setTimestamp(m.getTimestamp());
        aux.setCliId(m.getCliId());
        aux.setDst(m.getDst());
        receiveStep2Msg(aux);
    }

    @Override
    protected void receiveStep2Msg(SkeenMessage m){
        step2++;
        int maxTS = Math.max(LC, m.getTimestamp());
        LC += numNodes;
        if(LC < maxTS) LC = (maxTS - m.getSender()) + getId();

        if(m.getSender() != getId()) pending.add(m);
        if(receivedTimestamps.get(m.getId()) == null) receivedTimestamps.put(m.getId(), new ArrayList<>());
        receivedTimestamps.get(m.getId()).add(m);

        if(receivedTimestamps.get(m.getId()).size() == m.getDst().length){
            SkeenMessage max_ts = maxTimestampInPending(m);
            pending.removeIf(item->{return item.getId() == m.getId();});
            receivedTimestamps.remove(m.getId());
            ordered.add(max_ts);
        }
        
        for(;;){
            SkeenMessage min_ts = null;
            try {min_ts = ordered.first();}catch(NoSuchElementException e){}
            if(min_ts != null){
                SkeenMessage min_ts_pend = minTimestampInPending();
                if(min_ts_pend == null || min_ts.getTimestamp() < min_ts_pend.getTimestamp()){
                    ordered.remove(min_ts);
                    deliver(min_ts);
                }
                else {
                    break;
                }
            }
            else {
                break;
            }
        }
    }

    private void deliver(SkeenMessage m) {
        history.add(m);
        sendReply(m);
    }

    private SkeenMessage maxTimestampInPending(SkeenMessage m2) {
        Optional<SkeenMessage> opt_max_ts = receivedTimestamps.get(m2.getId()).stream()
        .max((item1, item2)->{return Integer.compare(item1.getTimestamp(), item2.getTimestamp());});
        return opt_max_ts.get();
    }

    private SkeenMessage minTimestampInPending() {
        Optional<SkeenMessage> opt_min_ts = pending.stream()
        .min((item1, item2)->{return Integer.compare(item1.getTimestamp(), item2.getTimestamp());});
        if(opt_min_ts.isPresent())
            return opt_min_ts.get();
        return null;
    }

    protected void finish(){
        if(bufferQueue.size() > 0){
            print("Queue is not empty !!! ");
            files.stop();
            exit();
        }
        print("Queue is empty ! =]");
        if(ordered.size() > 0) print("Warning: ordered is not empty... =[");
        if(pending.size() > 0) print("Warning: pending is not empty... =[");
        if(receivedTimestamps.size() > 0) print("Warning: receivedTimestamps is not empty... =[");
        print("-------------------------------------");
        print("Total msgs in the history:", history.size());
        print("Total local msgs received:", localMsgs);
        print("Total step1 received:", step1);
        print("Total step2 received:", step2);
        print("-------------------------------------");
        files.nodeFinished(getId());
        exit();
    }
}
