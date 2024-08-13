package util;

import java.util.Arrays;

public class MsgSize {
    private int id;
    private long time;
    private double size;
    private short [] dst;
    public int getId() {
        return id;
    }
    public long getTime() {
        return time;
    }
    public double getSize() {
        return size;
    }
    public MsgSize(long time, int id, double size, short[] dst) {
        this.time = time;
        this.id = id;
        this.size = size;
        this.dst = dst;
    }
    public String toString(){
        return time + ";" + id + ";" + size + ";" + dst.length + ";" + Arrays.toString(dst);
    }
}
