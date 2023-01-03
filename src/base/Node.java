package base;

import util.BaseObj;

public class Node extends BaseObj {
    private short id;
    private Host host;
    public Node() {}
    public Node(short id) {this.id = id;}
    public Node(short id, Host host) {
        this.id = id;
        this.host = host;
    }
    public short getId() {return id;}
    public Host getHost() {return host;}
    public void setHost(Host host) {this.host = host;}
    @Override
    public String toString(){
        return "Node"+getId()+" ["+getHost().getName()+":"+getHost().getPort()+"]";
    }
    @Override
    public boolean equals(Object n){
        return ((Node)n).getId() == getId();
    }
}
