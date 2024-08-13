package base;

import util.BaseObj;

public class Node extends BaseObj {
    private short id;
    private Host host;
    private int pos=-1;
    public Node() {}
    public Node(short id) {this.id = id;}
    public Node(short id, Host host, int pos) {
        this.id = id;
        this.host = host;
        this.pos = pos;
    }
    public short getId() {return id;}
    public int getPosition(){return pos;}
    public Host getHost() {return host;}
    public void setHost(Host host) {this.host = host;}
    @Override
    public String toString(){
        return "Node"+getId()+", pos: "+getPosition()+", ["+getHost().getName()+":"+getHost().getPort()+"]";
    }
    @Override
    public int hashCode() {
        return getId();
    }
    @Override
    public boolean equals(Object n){
        return ((Node)n).getId() == getId();
    }
}
