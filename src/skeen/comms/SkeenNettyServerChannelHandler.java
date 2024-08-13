package skeen.comms;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import skeen.messages.SkeenMessage;
import skeen.proxies.SkeenServerProxy;

public class SkeenNettyServerChannelHandler extends ChannelInboundHandlerAdapter {

    private SkeenServerProxy server;

    public SkeenNettyServerChannelHandler(SkeenServerProxy s){
        this.server = s;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        SkeenMessage m = (SkeenMessage)msg;
        m.setChannelIn(ctx.channel());
        server.buffer(m);
    }

    // @Override
    // public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    //     cause.printStackTrace();
    //     System.out.println("NettyServerChannelHandler - Exception -" + cause.toString());
    //     ctx.close();
    //     System.exit(0);
    // }
}
