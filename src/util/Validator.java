package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import base.Node;
import skeen.messages.LightMessage;
import org.javatuples.Pair;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphBuilder;
import org.jgrapht.graph.builder.GraphTypeBuilder;

public class Validator extends BaseObj {
    private FileManager files = new FileManager();
    private HashMap<Short, ArrayList<LightMessage>> allMessages = new HashMap<>();
    private List<Node> nodes = files.loadHosts();

    public void verifyCycles(){

        int totalMsgs = 0;
        // load messages from files
        for(Node n : nodes){
            ArrayList<LightMessage> msgs = files.loadMessages(n.getId(), false);
            if(msgs != null){
                totalMsgs += msgs.size();
                allMessages.put(n.getId(), msgs);
            }
            else {
                print("Error !!! Could not find messages for node", n.getId(), "!!!");
                exit();
            }
        }
        
        Graph<Integer,DefaultEdge> globalGraph = 
        GraphTypeBuilder.<Integer, DefaultEdge> directed()
        .allowingMultipleEdges(false)
        .allowingSelfLoops(false)
        .weighted(false)
        .edgeClass(DefaultEdge.class)
        .buildGraph();

        GraphBuilder<Integer,DefaultEdge,Graph<Integer,DefaultEdge>> builder = 
        new GraphBuilder<Integer,DefaultEdge,Graph<Integer,DefaultEdge>>(globalGraph);

        Set<Pair<Integer, Integer>> graphViz = new HashSet<>();
        
        // load the graph with messages delivered by each node
        for(Node n : nodes){
            ArrayList<LightMessage> delivered = allMessages.get(n.getId());
            boolean first = true;
            for(int idx = 0; idx < delivered.size(); idx++){
                builder.addVertex(delivered.get(idx).getId());
                if(!first) {
                    builder.addEdge(delivered.get(idx-1).getId(), delivered.get(idx).getId());
                    graphViz.add(new Pair<Integer,Integer>(delivered.get(idx-1).getId(), delivered.get(idx).getId()));
                }
                first = false;
            }
        }

        Graph<Integer, DefaultEdge> graph = builder.build();
        
        print("Cycle Validation Result:");
        print("-------------------------");
        print();

        print("Num Nodes:", nodes.size());
        print("Total msgs:", totalMsgs);
        print("Graph size:", graph.vertexSet().size());
        print();

        boolean cycle = new CycleDetector<>(graph).detectCycles();
        print("-------------------------");
        print("Contains cycle:", cycle);
        print("-------------------------");

        if(cycle){
            print("digraph G {");
            for(Pair<Integer,Integer> pair : graphViz){
                print("   ",pair.getValue0(),"->",pair.getValue1(), ";");
            }
            print("}");
        }

    }

    public static void main(String args []){
        new Validator().verifyCycles();
    }
}
