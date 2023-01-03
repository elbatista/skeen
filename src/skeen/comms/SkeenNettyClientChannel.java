package skeen.comms;

import java.util.concurrent.CyclicBarrier;
import base.Node;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import skeen.messages.SkeenMessageDecoder;
import skeen.messages.SkeenMessageEncoder;
import skeen.proxies.SkeenClientProxy;

public class SkeenNettyClientChannel extends Thread {

    private SkeenClientProxy serverProxy;
    private Node node;
    CyclicBarrier syncAllConnections;

    public SkeenNettyClientChannel(Node node, SkeenClientProxy serverProxy){
        this(node, serverProxy, null);
    }

    public SkeenNettyClientChannel(Node node, SkeenClientProxy serverProxy, CyclicBarrier syncAllConnections){
        this.syncAllConnections = syncAllConnections;
        this.node = node;
        this.serverProxy = serverProxy;
        start();
    }

    @Override
    public void run(){
        while(true){
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                b.group(workerGroup);
                b.channel(NioSocketChannel.class);
                b.option(ChannelOption.SO_KEEPALIVE, true);
                b.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new SkeenMessageDecoder(), new SkeenMessageEncoder(), 
                        new SkeenNettyClientChannelHandler(serverProxy, node.getId(), syncAllConnections));
                    }
                });
                Channel channel = null;
                while(channel == null){
                    try {
                        ChannelFuture future = b.connect(node.getHost().getName(), node.getHost().getPort()).sync();
                        channel = future.channel();
                    }
                    catch(Exception e){
                        try {Thread.sleep(2000);} catch (InterruptedException e2) {}
                    }
                }
                channel.closeFuture().sync();
            }
            catch (InterruptedException ex) {
                System.err.printf("Failed to create Netty communication system", ex);
            }
            finally {
                workerGroup.shutdownGracefully();
            }
        }
    }
}
