package skeen.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import io.netty.channel.Channel;
import util.BaseObj;
import util.OrderItem;

public class SkeenMessage extends BaseObj implements Externalizable, Comparable<SkeenMessage> {
    public enum Type {MSG, STEP1, STEP2, CONN, REPLY, END, READY}
    public enum TransactionType {NEW, PAYMENT, STATUS, DELIVERY, STOCK, NOPAYLOAD}
    private short sender = -1;
    private int id = -1, timestamp = -1, cliId = -1;
    private Type type;
    private short [] dst;

    // "transient" fields
    private Channel channelIn;
    private ArrayList<Integer> timeStamps = new ArrayList<>();

    //payload fields
    private TransactionType transaction;
    private Date orderDate;

    //neworder
    private ArrayList<OrderItem> items = new ArrayList<>();;

    //payment
    private double paymentAmount;

    //delivery and stocklevel
    private int carrierid_or_threshold;
    
    // constructors
    public SkeenMessage(){}
    public SkeenMessage(int id){
        this.id = id;
    }

    public TransactionType getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionType transaction) {
        this.transaction = transaction;
    }

    public ArrayList<OrderItem> getItems() {
        return items;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public void setPaymentAmount(double paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public void setCarrierid_or_threshold(int carrierid_or_threshold) {
        this.carrierid_or_threshold = carrierid_or_threshold;
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

    @Override
    public int hashCode() {
        return getId();
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
            case MSG: out.writeByte(0); writeExtPayload(out); break;
            case STEP1: out.writeByte(1); writeExtPayload(out); break;
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

    private void writeExtPayload(ObjectOutput out) throws IOException{
        switch(transaction){
            case NOPAYLOAD: {out.writeByte(10); return;}
            case NEW: {
                out.writeByte(1); 
                out.writeInt(items.size());
                for(OrderItem item : items){
                    out.writeInt(item.getId());
                    out.writeInt(item.getQty());
                }
                break;
            }
            case PAYMENT: {
                out.writeByte(2); 
                out.writeDouble(paymentAmount);
                break;
            }
            case STATUS: {
                out.writeByte(3);
                break;
            }
            case DELIVERY: {
                out.writeByte(4); 
                out.writeInt(carrierid_or_threshold);
                break;
            }
            case STOCK: {
                out.writeByte(5); 
                out.writeInt(carrierid_or_threshold);
                break;
            }
        }
        out.writeLong(orderDate.getTime());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = in.readInt();
        this.cliId = in.readInt();
        switch(in.readByte()){
            case 0: this.type = Type.MSG; readExtPayload(in); break;
            case 1: this.type = Type.STEP1; readExtPayload(in); break;
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

    private void readExtPayload(ObjectInput in) throws IOException {
        short transtype = in.readByte();
        switch(transtype){
            case 10: setTransaction(TransactionType.NOPAYLOAD); return;
            case 1: {
                setTransaction(TransactionType.NEW);
                int i = in.readInt();
                for(int j = 0; j < i; j++){
                    int itemid = in.readInt();
                    int qty = in.readInt();
                    items.add(new OrderItem(itemid, qty));
                }
                break;
            }
            case 2: {
                setTransaction(TransactionType.PAYMENT);
                paymentAmount = in.readDouble();
                break;
            }
            case 3: {
                setTransaction(TransactionType.STATUS);
                break;
            }
            case 4: {
                setTransaction(TransactionType.DELIVERY);
                carrierid_or_threshold = in.readInt();
                break;
            }
            case 5: {
                setTransaction(TransactionType.STOCK);
                carrierid_or_threshold = in.readInt();
                break;
            }
        }
        orderDate = new Date(in.readLong());
    }
    
    public short getMinDest() {
        return getDst()[0];
    }
}
