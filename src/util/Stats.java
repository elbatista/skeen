package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import skeen.messages.SkeenMessage.Type;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class Stats {
    private Vector<Long> values;
    private Vector<Boolean> isGlobal;
    private int accCount, limit;
    private int [] throughput;
    private int now = 0;
    private short numNodes=0;
    /**
     * Creates a new instance of Stats
     */
    public Stats() {
        values = new Vector<>();
        isGlobal = new Vector<>();
        accCount = 0;
    }

    public Stats(int duration, short numNodes) {
        this();
        this.numNodes = numNodes;
        throughput = new int[duration+1];
        System.out.println("Start tp measurements");
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run(){
                System.out.println("Tp at sec "+ now +": "+ throughput[now]);
                if(now < duration) now++;
            }
        }, 1000, 1000);
    }

    public int getCount() {
        return values.size();
    }

    public void store(long value, boolean isGlobal){
        values.add(value);
        this.isGlobal.add(isGlobal);
        accCount++;
        try{throughput[now]++;}catch(Exception e){}
    }

    public int getPartialCount() {
        int temp = accCount;
        accCount = 0;
        return temp;
    }

    public long getMax(int discardPercent) {
        Vector<Long> values = new Vector<>(this.values);
        int limit = discardPercent * values.size() / 100;
        Collections.sort(values);

        return values.get(values.size() - 1 - limit);
    }

    public double getAverage(int discardPercent) {
        Vector<Long> values = new Vector<>(this.values);
        int size = values.size();
        double sum = 0;

        //Collections.sort(values);
        limit = discardPercent * size / 100;
        if (size < 2 * limit)
            limit = 0;

        for (int i = limit; i < size - limit; i++) {
            sum = sum + values.get(i);
        }
        return sum / (size - 2 * limit);
    }

    public double getStdDev(int discardPercent) {
        Vector<Long> values = new Vector<>(this.values);
        int size = values.size();
        double avg = getAverage(discardPercent), var, quad = 0;
        long num = size - 2 * limit;

        //Collections.sort(values);
        for (int i = limit; i < size - limit; i++)
            quad += ((values.get(i) - avg) * (values.get(i) - avg));

        var = quad / (num - 1);
        return Math.sqrt(var);
    }

    public long getPercentile(int percentile, int discardPercent) {
        Vector<Long> values = new Vector<>(this.values);
        int size = values.size(), index;
        float percent = (float) percentile / 100;

        Collections.sort(values);
        limit = discardPercent * size / 100;
        if (size < 2 * limit)
            limit = 0;


        index = limit + Math.round(percent * (size - 2 * limit));
        if (index >= size)
            index = size - 1;

        return values.get(index);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public void persist(String fileName, int discardPercent) {
        File f = new File(fileName);
        long abs = 0;
        int order = 0;
        try {
            FileWriter fw = new FileWriter(f);
            fw.write("ORDER\tLATENCY\tABS\tTYPE\n");
            for (int i = 0; i < values.size(); i++) {
                abs += values.get(i);
                fw.write(++order + "\t" + values.get(i) + "\t" + abs + "\t" + (isGlobal.get(i) ? "global" : "local") /*+ "\t" + this.dstSize.get(i)*/ +  "\n");
            }
            fw.write("\n");
            fw.write(toString(discardPercent));
            fw.flush();
            fw.close();
        } catch (IOException ex) {
            System.err.println("Unable to save stats to file");
            ex.printStackTrace();
        }
    }

    private String toString(int discardPercent) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n--------------\nStats:\n");
        sb.append("\tTotal: " + getCount() + "\n");
        sb.append("\tMean: " + getAverage(discardPercent) + "\n");
        sb.append("\tStdDev: " + getStdDev(discardPercent) + "\n");
        sb.append("\tMax: " + getMax(discardPercent) + "\n");
        sb.append("\tPercentile (25, 50, 75, 95, 99): (" + getPercentile(25, discardPercent) + ", " +
                getPercentile(50, discardPercent) + ", " + getPercentile(75, discardPercent) + ", " +
                getPercentile(95, discardPercent) + ", " + getPercentile(99, discardPercent) + ")\n--------------\n\n");
        return sb.toString();
    }


    ////////

    class ValuesPerNode {
        long [] values;
        short[] dsts;
        boolean isGlobal;
        Type type;
        public ValuesPerNode(long [] values, boolean isGlobal, short[] dsts, Type type) {
            this.values = values;
            this.isGlobal = isGlobal;
            this.dsts = dsts;
            this.type = type;
        }
    }

    ArrayList<ValuesPerNode> valuesPerNode = new ArrayList<>();

    public void store(HashMap<Short, Long> latsPerNode, boolean isGlobal, short[] dsts, Type type) {
        long [] val = new long[numNodes];
        for(short node : latsPerNode.keySet()){
            val[node] = latsPerNode.get(node);
        }
        valuesPerNode.add(new ValuesPerNode(val, isGlobal, dsts, type));
    }

    public void persistPerNodes(String fileName, int discardPercent) {
        File f = new File(fileName);
        int order = 0;
        try {
            FileWriter fw = new FileWriter(f);

            fw.write("ORDER\t");
            for(int i = 0; i < numNodes; i++) fw.write("LAT_"+i+"\t");
            fw.write("DSTS\t");
            fw.write("TYPE\t");
            fw.write("MSGTYPE\n");

            for (int i = 0; i < valuesPerNode.size(); i++) {
                ValuesPerNode value = valuesPerNode.get(i);
                fw.write(++order + "\t");
                for(int j = 0; j < numNodes; j++){
                    fw.write(value.values[j] + "\t");
                }
                String dsts="[";

                for(int x=0; x<value.dsts.length; x++){
                    if(x>0) dsts += ",";
                    dsts += value.dsts[x];
                }

                dsts+="]";
                fw.write(dsts + "\t" + (value.isGlobal ? "global" : "local")+ "\t" + value.type  + "\n");
            }

            fw.write("\n");
            fw.write(toString(discardPercent));
            fw.flush();
            fw.close();
        } catch (IOException ex) {
            System.err.println("Unable to save stats to file");
            ex.printStackTrace();
        }
    }
}
