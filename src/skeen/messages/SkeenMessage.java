package skeen.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import io.netty.channel.Channel;
import util.BaseObj;

public class SkeenMessage extends BaseObj implements Externalizable, Comparable<SkeenMessage> {
    public enum Type {MSG, STEP1, STEP2, CONN, REPLY, END, READY}
    private short sender = -1;
    private int id = -1, timestamp = -1, cliId = -1;
    private Type type;
    private short [] dst;

    // "transient" fields
    private Channel channelIn;
    private ArrayList<Integer> timeStamps = new ArrayList<>();
    
    // constructors
    public SkeenMessage(){}
    public SkeenMessage(int id){
        this.id = id;
    }

    // methods
    public int getId() {
        return id;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public ArrayList<Integer> getTimeStamps() {
        return timeStamps;
    }
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public Channel getChannelIn() {
        return channelIn;
    }
    public void setChannelIn(Channel channelIn) {
        this.channelIn = channelIn;
    }

    public int getCliId() {
        return cliId;
    }

    public void setCliId(int cliId) {
        this.cliId = cliId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public short getSender() {
        return sender;
    }

    public void setSender(short sender) {
        this.sender = sender;
    }

    public short[] getDst() {
        return dst;
    }

    public void setDst(short... dst) {
        this.dst = dst;
    }

    public boolean equals(Object m){
        return ((SkeenMessage)m).getId() == getId();
    }

    public boolean isAddressedTo(short d){
        for(short i : dst)
            if (i == d)
                return true;
        return false;
    }

    public String toString(){
        return toString(getId(), Arrays.toString(getDst()));
    }

    @Override
    public int compareTo(SkeenMessage o) {
        if(this.timestamp < o.getTimestamp()) return -1;
        if(this.timestamp > o.getTimestamp()) return 1;
        return Integer.valueOf(this.id).compareTo(o.getId());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(this.id);
        out.writeInt(this.cliId);
        switch(this.type){
            case MSG: out.writeByte(0); break;
            case STEP1: out.writeByte(1); break;
            case STEP2: out.writeByte(2); break;
            case CONN: out.writeByte(3); break;
            case REPLY: out.writeByte(4); break;
            case READY: out.writeByte(9); break;
            case END: out.writeByte(10); break;
        }
        out.writeByte(this.sender);
        out.writeInt(this.timestamp);
        if(this.dst == null){
            out.writeInt(0);
        }
        else {
            out.writeInt(this.dst.length);
            for(short i : this.dst)
                out.writeByte(i);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = in.readInt();
        this.cliId = in.readInt();
        switch(in.readByte()){
            case 0: this.type = Type.MSG; break;
            case 1: this.type = Type.STEP1; break;
            case 2: this.type = Type.STEP2; break;
            case 3: this.type = Type.CONN; break;
            case 4: this.type = Type.REPLY; break;
            case 9: this.type = Type.READY; break;
            case 10: this.type = Type.END; break;
        }
        this.sender = in.readByte();
        this.timestamp = in.readInt();
        int dstLen = in.readInt();
        if(dstLen > 0){
            this.dst = new short[dstLen];
            for(int i = 0; i < dstLen; i++)
                this.dst[i] = in.readByte();
        }
    }
    
    public short getMinDest() {
        return getDst()[0];
    }
}
