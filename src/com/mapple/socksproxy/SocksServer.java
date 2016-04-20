/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mapple.socksproxy;

import com.mapple.forward.client.TcpForwardClientInitializer;
import com.mapple.forward.server.TcpForwardServerInitializer;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public final class SocksServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "10010"));
    static final int PORT2 = Integer.parseInt(System.getProperty("tcpforword", "10011"));

    public static void main(String[] args) throws Exception {
        System.out.println("TEST-----------------------------");
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        EventLoopGroup boss2Group = new NioEventLoopGroup(1);
        EventLoopGroup worker2Group = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_REUSEADDR, true)
//             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new SocksServerInitializer());
            ChannelFuture futrue = b.bind(PORT).sync().channel().closeFuture();
            
            ServerBootstrap forword = new ServerBootstrap();
            forword.group(boss2Group, worker2Group)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_REUSEADDR, true)
//             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new TcpForwardServerInitializer())
             .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture futrue2 = forword.bind(PORT2).sync().channel().closeFuture();
            
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
        } finally {
            bossGroup.shutdownGracefully();
            boss2Group.shutdownGracefully();
            workerGroup.shutdownGracefully();
            worker2Group.shutdownGracefully();
        }
        
        
    }
}
