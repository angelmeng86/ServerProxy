package com.mapple;

import java.util.concurrent.ThreadFactory;

import com.mapple.forward.client.TcpForwardClientInitializer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;

public class ForwardClientMain {
    
    public static boolean running = true;
    
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
        
        udtClient(ip, port, userName);
    }
    
    private static void tcpClient(String ip, int port, String userName) {
    	 // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bs = new Bootstrap();
            bs.group(group)
             .channel(NioSocketChannel.class)
             .option(ChannelOption.TCP_NODELAY, true)
             .handler(new TcpForwardClientInitializer(userName));

            while(running) {
                // Start the client.
                try {
                    ChannelFuture f = bs.connect(ip, port).sync();
                    System.out.println("Tcp Connect " + ip + ":" + port);
                    // Wait until the connection is closed.
                    f.channel().closeFuture().sync();
                    System.out.println("Tcp Disconnect " + ip + ":" + port);
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
    
    private static void udtClient(String ip, int port, String userName) {
   	 // Configure the client.
       ThreadFactory connectFactory = new DefaultThreadFactory("connect");
       EventLoopGroup group = new NioEventLoopGroup(1,
               connectFactory, NioUdtProvider.BYTE_PROVIDER);
       try {
           Bootstrap bs = new Bootstrap();
           bs.group(group)
            .channelFactory(NioUdtProvider.BYTE_CONNECTOR)
            .handler(new TcpForwardClientInitializer(userName));

           while(running) {
               // Start the client.
               try {
                   ChannelFuture f = bs.connect(ip, port).sync();
                   System.out.println("Udt Connect " + ip + ":" + port);
                   // Wait until the connection is closed.
                   f.channel().closeFuture().sync();
                   System.out.println("Udt Disconnect " + ip + ":" + port);
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
    
    /*private static String getAddr(String ip) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://apis.baidu.com/apistore/iplookupservice/iplookup?ip=")
                .addHeader("apikey", "ce45a88f920769bda3a789876300b6c4")
                .build();

        Response response;
        try {
            response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }*/

}
