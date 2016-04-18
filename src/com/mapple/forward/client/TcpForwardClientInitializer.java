package com.mapple.forward.client;

import com.mapple.forward.ForwardBeatEncoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class TcpForwardClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
    	ch.pipeline().addLast(
    			ForwardLoginEncoder.INSTANCE,
                ForwardBeatEncoder.INSTANCE);
    	
        ch.pipeline().addLast(
                new LoggingHandler(LogLevel.DEBUG),
                new TcpForwardClientDecoder());
    }

}
