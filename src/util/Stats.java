package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class Stats {
    private Vector<Long> values;
    private Vector<Boolean> isGlobal;
    //private Vector<Integer> dstSize;
    private int accCount, limit;
    // private Vector<Value> valuesPerNode;
    // private int numNodes;

    private int [] throughput;
    private int now = 0;

    class Value {
        short node;
        long value;
        boolean isGlobal;
        //int dstSize;
        public Value(short node, long value, boolean isGlobal, int dstSize) {
            this.node = node;
            this.value = value;
            this.isGlobal = isGlobal;
            //this.dstSize = dstSize;
        }
    }

    /**
     * Creates a new instance of Stats
     */
    public Stats(int duration) {
        values = new Vector<>();
        // valuesPerNode = new Vector<>();
        isGlobal = new Vector<>();
        //dstSize = new Vector<>();
        accCount = 0;
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

    public void store(long value, boolean isGlobal){//, int dstSize) {
        values.add(value);
        this.isGlobal.add(isGlobal);
        //this.dstSize.add(dstSize);
        accCount++;
        try{throughput[now]++;}catch(Exception e){}
    }

    // public void store(short node, long value, boolean isGlobal, int dstSize) {
    //     valuesPerNode.add(new Value(node, value, isGlobal, dstSize));
    // }

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
        // File f = new File(fileName.replace(".txt", "Geral.txt"));
        File f = new File(fileName);
        long abs = 0;
        int order = 0;
        try {
            FileWriter fw = new FileWriter(f);
            // fw.write("ORDER\tLATENCY\tABS\tTYPE\tDSTSIZE\n");
            fw.write("ORDER\tLATENCY\tABS\tTYPE\n");
            for (int i = 0; i < values.size(); i++) {
                abs += values.get(i);
                fw.write(++order + "\t" + values.get(i) + "\t" + abs + "\t" + (isGlobal.get(i) ? "global" : "local") /*+ "\t" + this.dstSize.get(i)*/ +  "\n");
            }

            fw.write("\n");
            fw.write(toString(discardPercent));
            fw.flush();
            fw.close();

            // separate latencies
            // FileWriter [] files = new FileWriter[numNodes];
            // for(int i=0; i < numNodes; i++) 
            //     files[i] = new FileWriter(new File(fileName.replace(".txt", "Node"+i+".txt")));
            
            // abs = 0;
            // order = 0;
            
            // for(int i=0; i < numNodes; i++)
            //     files[i].write("ORDER\tLATENCY\tABS\tTYPE\tDSTSIZE\n");

            // for (int i = 0; i < valuesPerNode.size(); i++) {
            //     abs += valuesPerNode.get(i).value;
            //     files[valuesPerNode.get(i).node].write(++order +  "\t" + valuesPerNode.get(i).value + "\t" + abs + "\t" + (valuesPerNode.get(i).isGlobal ? "global" : "local") + "\t" + valuesPerNode.get(i).dstSize + "\n");
            // }

            // for(int i=0; i < numNodes; i++){
            //     files[i].flush();
            //     files[i].close();
            // }

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
}


