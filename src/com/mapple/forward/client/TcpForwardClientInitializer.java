package com.mapple.forward.client;

import com.mapple.forward.ForwardBeatEncoder;
import com.mapple.forward.ForwardLogin;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class TcpForwardClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
//        ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(60, 30, 0));
//        ch.pipeline().addLast(ForwardBeatHandler.INSTANCE);
        ch.pipeline().addLast(
                new LoggingHandler(LogLevel.DEBUG),
                new TcpForwardClientDecoder(),
                ForwardLoginEncoder.INSTANCE,
                new ForwardBeatEncoder());
        
        ch.writeAndFlush(new ForwardLogin("LWZ"));
    }

}
