package com.mapple;

import java.util.concurrent.ThreadFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import com.mapple.forward.server.TcpForwardServerInitializer;
import com.mapple.http.HttpHelloWorldServerInitializer;
import com.mapple.socksproxy.SocksServerInitializer;

public class ForwardServerMain {

	static final int PORT = Integer.parseInt(System.getProperty("port", "10010"));
    static final int PORT2 = Integer.parseInt(System.getProperty("tcpforword", "10011"));
    static final int PORT4 = Integer.parseInt(System.getProperty("udpforword", "10013"));
    static final int PORT3 = Integer.parseInt(System.getProperty("http", "10012"));

    public static void main(String[] args) throws Exception {
        System.out.println("SERVER TEST-----------------------------");
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        EventLoopGroup boss2Group = new NioEventLoopGroup(1);
        EventLoopGroup worker2Group = new NioEventLoopGroup();
        
        EventLoopGroup boss3Group = new NioEventLoopGroup(1);
        EventLoopGroup worker3Group = new NioEventLoopGroup();
        
        ThreadFactory acceptFactory = new DefaultThreadFactory("accept");
        ThreadFactory connectFactory = new DefaultThreadFactory("connect");
        EventLoopGroup acceptGroup = new NioEventLoopGroup(1, acceptFactory, NioUdtProvider.BYTE_PROVIDER);
        EventLoopGroup connectGroup = new NioEventLoopGroup(1, connectFactory, NioUdtProvider.BYTE_PROVIDER);
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_REUSEADDR, true)
             .option(ChannelOption.SO_BACKLOG, 64)
//             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new SocksServerInitializer());
            ChannelFuture futrue = b.bind(PORT).sync().channel().closeFuture();
            
            ServerBootstrap forword = new ServerBootstrap();
            forword.group(boss2Group, worker2Group)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_REUSEADDR, true)
             .option(ChannelOption.SO_BACKLOG, 64)
//             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new TcpForwardServerInitializer())
             .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture futrue2 = forword.bind(PORT2).sync().channel().closeFuture();
            
            ServerBootstrap http = new ServerBootstrap();
            http.option(ChannelOption.SO_BACKLOG, 32);
            http.group(boss3Group, worker3Group)
             .channel(NioServerSocketChannel.class)
//             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new HttpHelloWorldServerInitializer());

            ChannelFuture futrue3 = http.bind(PORT3).sync().channel().closeFuture();

            System.out.println("Open your web browser and navigate to " + "http://IP:" + PORT3 + '/');
            
            ServerBootstrap boot4 = new ServerBootstrap();
            boot4.group(acceptGroup, connectGroup)
                    .channelFactory(NioUdtProvider.BYTE_ACCEPTOR)
                    .option(ChannelOption.SO_BACKLOG, 32)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new TcpForwardServerInitializer());
            // Start the server.
            ChannelFuture future4 = boot4.bind(PORT4).sync().channel().closeFuture();

         // Configure the client.
            /*
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap bs = new Bootstrap();
                bs.group(group)
                 .channel(NioSocketChannel.class)
                 .option(ChannelOption.TCP_NODELAY, true)
                 .handler(new TcpForwardClientInitializer("LWZ"));

                // Start the client.
                ChannelFuture f = bs.connect("127.0.0.1", PORT2).sync();
                System.out.println("Connect " + PORT2);
                // Wait until the connection is closed.
                f.channel().closeFuture().sync();
            } catch(Exception e) {
                e.printStackTrace();
            }
            finally {
                // Shut down the event loop to terminate all threads.
                group.shutdownGracefully();
            }
            System.out.println("Client Close " + PORT2);
            */
            
            futrue.sync();
            futrue2.sync();
            futrue3.sync();
            future4.sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            boss2Group.shutdownGracefully();
            worker2Group.shutdownGracefully();
            boss3Group.shutdownGracefully();
            worker3Group.shutdownGracefully();
            
            acceptGroup.shutdownGracefully();
            connectGroup.shutdownGracefully();
        }
        
        
    }

}
