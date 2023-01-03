package util;
public class BaseObj {
    protected void exit(){System.exit(0);}
    protected void sleep(long millis){try{Thread.sleep(millis);}catch(Exception e){}}
    protected void print(Object... args){System.out.println(toString(args));}
    protected String toString(Object... args){
        String s = "";
        for(Object obj : args)s+=String.valueOf((obj==null?"":obj))+" ";
        return s;
    }
}
