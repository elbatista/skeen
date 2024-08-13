package skeen.messages;

import java.io.Serializable;
import java.util.Arrays;
import util.BaseObj;

public class LightMessage extends BaseObj implements Serializable {
    private int id = -1;
    private short [] dst;

    public LightMessage(int id){
        this.id = id;
    }

    public LightMessage(int id, short [] dst){
        this.id = id;
        setDst(dst);
    }

    public int getId() { return id; }
    
    public short[] getDst() { return dst; }

    public void setDst(short[] dst) {
        this.dst = dst;
    }

    public int getLca(){return this.dst[0];}

    public boolean isAddressedTo(short d){
        for(short i : dst) if (i == d) return true;
        return false;
    }

    public short getLcd(SkeenMessage m){
        short lcd = -1;
        for (short dst1 : getDst()) {
            for(short dst2 : m.getDst()){
                if(dst1 < dst2) break;
                if(dst1 == dst2) return dst1;
            }
        }
        return lcd;
    }

    @Override
    public int hashCode() {
        return getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LightMessage other = (LightMessage) obj;
        if (id != other.id)
            return false;
        return true;
    }

    @Override
    public String toString(){
        return toString(getId(), Arrays.toString(getDst()));
    }
}
