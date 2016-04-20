package com.mapple;

import com.mapple.forward.client.TcpForwardClientInitializer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.NetUtil;

public class ForwardClientMain {
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        if(args.length != 3) {
            System.out.println("useage: java -jar forwardclient.jar [USERNAME] [IP] [PORT]");
            return;
        }
        
        String userName = args[0];
        String ip = args[1];
        int port = Integer.parseInt(args[2]);
        
        if (!NetUtil.isValidIpV4Address(ip)) {
            throw new IllegalArgumentException("ip: " + ip + " (expected: a valid IPv4 address)");
        }
        
        if (port <= 0 || port >= 65536) {
            throw new IllegalArgumentException("port: " + port + " (expected: 1~65535)");
        }
        
     // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bs = new Bootstrap();
            bs.group(group)
             .channel(NioSocketChannel.class)
             .option(ChannelOption.TCP_NODELAY, true)
             .handler(new TcpForwardClientInitializer(userName));

            while(true) {
                // Start the client.
                try {
                    ChannelFuture f = bs.connect(ip, port).sync();
                    System.out.println("Connect " + ip + ":" + port);
                    // Wait until the connection is closed.
                    f.channel().closeFuture().sync();
                    System.out.println("Disconnect " + ip + ":" + port);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Thread.sleep(5000);
                System.out.println("Retry...");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }

}
