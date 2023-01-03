package skeen.messages;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class SkeenMessageDecoder extends ReplayingDecoder<SkeenMessage> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int size = in.readInt();
        byte[] data = new byte[size];
        in.readBytes(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        try {
            SkeenMessage m = (SkeenMessage)ois.readObject();
            out.add(m);
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("MessageDecoder - Exception - " + e);
            System.exit(0);
        }
    }
}