package base;

public class Host {
    private String name;
    private int port;
    public Host(String name, int port){
        this.name = name;
        this.port = port;
    }
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public int getPort() {return port;}
    public void setPort(int port) {this.port = port;}
    public String toString(){
        return "["+name+":"+port+"]";
    }
}
