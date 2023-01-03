package skeen.messages;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class SkeenMessageEncoder extends MessageToByteEncoder<SkeenMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, SkeenMessage msg, ByteBuf out) throws Exception {
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(msg);
            byte [] data = baos.toByteArray();
            out.writeInt(data.length);
            out.writeBytes(data);
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.print("MessageEncoder - Excpetion - "+ e);
            System.exit(0);
        }
    }
}