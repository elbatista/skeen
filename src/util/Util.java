package util;

import java.util.Random;

public class Util extends BaseObj{
    private static Util instance;
    private char[] symbols= "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    public byte[] randomData() {
        Random rand = new Random();
        int len = rand.nextInt(1024)+1;
        char[] buf = new char[len];
        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[rand.nextInt(symbols.length)];
        return new String(buf).getBytes();
    }

    public static Util getInstance(){
        if(instance == null) instance = new Util();
        return instance;
    }

}
