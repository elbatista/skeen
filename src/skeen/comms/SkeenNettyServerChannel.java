package skeen.comms;

import java.net.InetSocketAddress;
import base.Node;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import skeen.messages.SkeenMessageDecoder;
import skeen.messages.SkeenMessageEncoder;
import skeen.proxies.SkeenServerProxy;

public class SkeenNettyServerChannel extends Thread {
    private Node node;
    private SkeenServerProxy server;

    public SkeenNettyServerChannel(Node node, SkeenServerProxy server) {
        this.node = node;
        this.server = server;
        start();
    }

    @Override
    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); 
            b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new SkeenMessageDecoder(), new SkeenMessageEncoder(),
                    new SkeenNettyServerChannelHandler(server));
                }
            })
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true);
            Channel channel = b.bind(new InetSocketAddress(node.getHost().getName(), node.getHost().getPort())).sync().channel();
            channel.closeFuture().sync();
        }
        catch (InterruptedException ex) {
            System.err.printf("Failed to create Netty communication system", ex);
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
