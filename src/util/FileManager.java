package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import base.Host;
import base.Node;

public class FileManager extends BaseObj {
    private String sep = System.getProperty("file.separator");
    public ArrayList<Node> loadHosts(){
        try{
            ArrayList<Node> nodes = new ArrayList<>();
            FileReader fr = new FileReader("config"+sep+"hosts.config");
            BufferedReader rd = new BufferedReader(fr);
            String line = null;
            while((line = rd.readLine()) != null){
                if(!line.startsWith("#")){ // ignore comments
                    StringTokenizer str = new StringTokenizer(line, " ");
                    if(str.countTokens() > 2){
                        short id = Short.valueOf(str.nextToken());
                        String hostName = str.nextToken();
                        int port = Integer.valueOf(str.nextToken());
                        Host host = new Host(hostName, port);
                        Node node = new Node(id, host);
                        nodes.add(node);
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

        print("Created zones file");
        
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

        print("Created random latencies file");
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
            print("Node", id, "is finished");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
