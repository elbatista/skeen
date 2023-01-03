package skeen.comms;

import java.util.concurrent.CyclicBarrier;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import skeen.messages.SkeenMessage;
import skeen.messages.SkeenMessage.Type;
import skeen.proxies.SkeenClientProxy;

public class SkeenNettyClientChannelHandler extends ChannelInboundHandlerAdapter {
    private SkeenClientProxy proxy;
    private short dst;
    private CyclicBarrier syncAllConnections;

    public SkeenNettyClientChannelHandler(SkeenClientProxy p, short dst, CyclicBarrier syncAllConnections){
        this.proxy = p;
        this.dst = dst;
        this.syncAllConnections = syncAllConnections;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        proxy.setChannelToDest(ctx.channel(), dst);
        if(syncAllConnections != null) 
            syncAllConnections.await();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        SkeenMessage m = (SkeenMessage)msg;
        if(m.getType() == Type.CONN){
            proxy.receiveReplyInitMsg(m);
            return;
        }
        if(m.getType() == Type.READY){
            proxy.receiveReplyReadyMsg(m);
            return;
        }
        if(m.getType() == Type.END){
            proxy.receiveReplyEndMsg(m);
            return;
        }
        proxy.receiveReply(m);
    }
}
