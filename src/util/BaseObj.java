package util;
public class BaseObj {
    private boolean print=true;
    protected void setPrint(boolean p){print=p;}
    protected void exit(){System.exit(0);}
    protected void sleep(long millis){try{Thread.sleep(millis);}catch(Exception e){}}
    protected void print(Object... args){if(print) System.out.println(toString(args));}
    protected void printF(Object... args){System.out.println(toString(args));}
    protected String toString(Object... args){
        String s = "";
        for(Object obj : args)s+=String.valueOf((obj==null?"":obj))+" ";
        return s;
    }
}
