package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import org.javatuples.Pair;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import base.Host;
import base.Node;
import skeen.messages.LightMessage;
import skeen.messages.LightMessagesList;

public class FileManager extends BaseObj {
    private String sep = System.getProperty("file.separator");
    public ArrayList<Node> loadHosts(){
        try{
            ArrayList<Node> nodes = new ArrayList<>();
            FileReader fr = new FileReader("config"+sep+"hosts.config");
            BufferedReader rd = new BufferedReader(fr);
            String line = null;
            int pos = 0;
            while((line = rd.readLine()) != null){
                if(!line.startsWith("#")){ // ignore comments
                    StringTokenizer str = new StringTokenizer(line, " ");
                    if(str.countTokens() > 2){
                        short id = Short.valueOf(str.nextToken());
                        String hostName = str.nextToken();
                        int port = Integer.valueOf(str.nextToken());
                        Host host = new Host(hostName, port);
                        Node node = new Node(id, host, pos);
                        nodes.add(node);
                        pos++;
                    }
                }
            }
            fr.close();
            rd.close();
            return nodes;
        }
        catch(Exception e){
            e.printStackTrace(System.out);
            return null;
        }
    }

    public void persistMessages(LightMessagesList msgs, short id, boolean cli, boolean msgsToSend) {
        try {
            ArrayList<LightMessage> list = new ArrayList<>();
            FileOutputStream fileOut;
            ObjectOutputStream objectOut;
            PrintWriter printerOut = null;

            if(msgsToSend)
                printerOut = new PrintWriter("files"+sep+"msgsToSend"+id+".txt");

            LightMessagesList.Item item = msgs.getFirst();
            while(item != null){
                list.add(item.get());
                if(msgsToSend)
                    printerOut.println(item.get().getId() + " " + Arrays.toString(item.get().getDst()).replace("[", "").replace("]", "").replace(" ", ""));
                item = item.getNext();
            }

            if(msgsToSend){
                printerOut.flush();
                printerOut.close();
            }

            fileOut = new FileOutputStream("files"+sep+(cli?"Cli":"Node")+id+".msgs");
            objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(list);
            objectOut.flush();
            objectOut.close();
            fileOut.close();

            printF("Messages persisted");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList<LightMessage> loadMessages(short id, boolean cli) {
        try {
            ArrayList<LightMessage> msgs = null;
            FileInputStream fis = new FileInputStream("files"+sep+(cli?"Cli":"Node")+id+".msgs");
            ObjectInputStream ois = new ObjectInputStream(fis);
            msgs = (ArrayList<LightMessage>) ois.readObject();
            ois.close();
            fis.close();
            return msgs;
        } catch (Exception ex) {
            ex.printStackTrace();
           return null;
        }
    }

    public List<short[]> getMsgsToSend(short cli) {
        Scanner scan=null;
        ArrayList<short[]> list = new ArrayList<>();
        try {
            scan = new Scanner(new File("files"+sep+"msgsToSend"+cli+".txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while(scan.hasNextLine()){
            String line = scan.nextLine();
            if(line.startsWith("#") || line.startsWith("//")) continue;
            String [] aux = line.split(" ")[1].split(",");
            short [] dst = new short[aux.length];
            for(int i = 0; i < dst.length; i++)
                dst[i] = Short.valueOf(aux[i]);
            list.add(dst);
        }

        return list;
    }

    public void generateRandLatencies(short ms){
        ArrayList<Node> hosts = loadHosts();

        // cria arquivo de zonas e ips
        PrintWriter fileOut;
        try {
            fileOut = new PrintWriter("wan"+System.getProperty("file.separator")+"ips.csv");
            fileOut.println("Zone,IP");
            for(Node n : hosts)
                fileOut.println("Zone"+n.getId()+","+n.getHost().getName());
            fileOut.flush();
            fileOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        printF("Created zones file");
        
        // cria arquivo de latencias entre zonas
        try {
            fileOut = new PrintWriter("wan"+System.getProperty("file.separator")+"latencies.csv");
            fileOut.print("From/to,");
            for(int i=0; i<hosts.size();i++)
                fileOut.print("Zone"+hosts.get(i).getId()+(i < (hosts.size()-1) ? "," : ""));
            fileOut.println();
            for(int i = 0; i < hosts.size(); i++){
                Node n = hosts.get(i);
                fileOut.print("Zone" + n.getId() + ",");
                for(int j = 0; j < hosts.size(); j++){
                    Node n2 = hosts.get(j);
                    if(n2.getId() == n.getId())
                        fileOut.print(0+(j < (hosts.size()-1) ? "," : ""));
                    else 
                        fileOut.print(new Random().nextInt(ms)+(j < (hosts.size()-1) ? "," : ""));
                }
                fileOut.println();
            }
            fileOut.flush();
            fileOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        printF("Created random latencies file");
    }

    public static void main(String ... args){
        new FileManager().generateRandLatencies(Short.valueOf(args[0]));
    }

    public void stop() {
        PrintWriter fileOut;
        try {
            fileOut = new PrintWriter("files"+System.getProperty("file.separator")+"stop");
            fileOut.println("true");
            fileOut.flush();
            fileOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void nodeFinished(short id) {
        try {
            PrintWriter printerOut = new PrintWriter("files"+sep+"NodeFinished"+id+".txt");
            printerOut.println(true);
            printerOut.flush();
            printerOut.close();
            printF("Node", id, "is finished");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Collection<Pair<Short, Short>> loadByzCastTree(ArrayList<String[]> mappings, short id) {
        try{
            ArrayList<Pair<Short, Short>> pairs = new ArrayList<>();
            FileReader fr = new FileReader("config"+sep+"byzcast.config");
            BufferedReader rd = new BufferedReader(fr);
            String line = null;

            while((line = rd.readLine()) != null){
                if(line.startsWith("-")) break; // gonna start reading the mappings
                if(!line.startsWith("#")){ // ignore comments #
                    StringTokenizer str = new StringTokenizer(line, "->");
                    if(str.countTokens() > 1){
                        short id1 = Short.valueOf(str.nextToken());
                        short id2 = Short.valueOf(str.nextToken());
                        pairs.add(new Pair<Short,Short>(id1, id2));
                        print("loadByzCastTree, read pair", id1, id2);
                    }
                }
            }

            while((line = rd.readLine()) != null){
                if(line.startsWith("#") || line.isEmpty()) continue; // ignore comments #
                String [] map = line.split(",");
                if(map != null && map.length > 0 && Short.valueOf(map[0]) == id){
                    mappings.add(map);
                    print("loadByzCastTree, read map", line);
                }
            }

            fr.close();
            rd.close();
            return pairs;
        }
        catch(Exception e){
            e.printStackTrace(System.out);
            return null;
        }
    }

    public Graph<Short,DefaultEdge> loadByzCastTreeAsGraph() {
        try{
            Graph<Short,DefaultEdge> graph = GraphTypeBuilder.<Short, DefaultEdge> directed()
            .allowingMultipleEdges(false)
            .allowingSelfLoops(false)
            .weighted(false)
            .edgeClass(DefaultEdge.class)
            .buildGraph();

            FileReader fr = new FileReader("config"+sep+"byzcast.config");
            BufferedReader rd = new BufferedReader(fr);
            String line = null;
            while((line = rd.readLine()) != null){
                if(line.startsWith("-")) break; // skip the mappings
                if(!line.startsWith("#")){ // ignore comments #
                    StringTokenizer str = new StringTokenizer(line, "->");
                    if(str.countTokens() > 1){
                        short id1 = Short.valueOf(str.nextToken());
                        short id2 = Short.valueOf(str.nextToken());
                        graph.addVertex(id1);
                        graph.addVertex(id2);
                        graph.addEdge(id1, id2);
                        print("loadByzCastTreeAsGraph, read pair", id1, id2);
                    }
                }
            }
            fr.close();
            rd.close();
            return graph;
        }
        catch(Exception e){
            e.printStackTrace(System.out);
            return null;
        }
    }

    public void persistMsgSizes(ArrayList<MsgSize> sizes, short id) {
        try {
            PrintWriter printerOut = new PrintWriter("files"+sep+"Node"+id+"MsgSizes.txt");
            for(MsgSize size : sizes) printerOut.println(size);
            printerOut.flush();
            printerOut.close();
            printF("Created msg sizes file");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void loadLocalityFile(HashMap<Short, String> nearestWHs) {
        try{

            FileReader fr = new FileReader("config/locality.conf");
            BufferedReader rd = new BufferedReader(fr);
            String line = null;
            while((line = rd.readLine()) != null){
                if(!line.startsWith("#")){ // ignore comments #
                    StringTokenizer str = new StringTokenizer(line, "-");
                    if(str.countTokens() > 1){
                        short id = Short.valueOf(str.nextToken().trim());
                        String map = str.nextToken().trim();
                        nearestWHs.put(id, map);
                    }
                }
            }
            fr.close();
            rd.close();
            //return graph;
        }
        catch(Exception e){
            e.printStackTrace(System.out);
            //return null;
        }
    }
}
