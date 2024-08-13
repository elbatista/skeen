package skeen.messages;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.ArrayList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import util.MsgSize;

public class SkeenMessageDecoder extends ReplayingDecoder<SkeenMessage> {
    private ArrayList<MsgSize> sizes;

    public SkeenMessageDecoder(ArrayList<MsgSize> sizes){
        this.sizes = sizes;
    }
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int size = in.readInt();
        byte[] data = new byte[size];
        in.readBytes(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        try {
            SkeenMessage m = (SkeenMessage)ois.readObject();

            if(sizes != null && m.getType() == SkeenMessage.Type.MSG || m.getType() == SkeenMessage.Type.STEP1 || m.getType() == SkeenMessage.Type.STEP2){
                sizes.add(new MsgSize(System.nanoTime(), m.getId(), (double)size, m.getDst()));
            }

            out.add(m);
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("MessageDecoder - Exception - " + e);
            System.exit(0);
        }
    }
}