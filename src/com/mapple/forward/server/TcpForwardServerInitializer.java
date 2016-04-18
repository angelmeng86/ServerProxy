package com.mapple.forward.server;

import com.mapple.forward.ForwardBeatEncoder;
import com.mapple.forward.ForwardBeatHandler;
import com.mapple.forward.ForwardDataEncoder;
import com.mapple.forward.ForwardDisconnectEncoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class TcpForwardServerInitializer extends ChannelInitializer<SocketChannel> {

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
	    System.out.println("新连接进来-----");
	    
		ch.pipeline().addLast(
		        ForwardLoginAckEncoder.INSTANCE,
		        ForwardConnectEncoder.INSTANCE,
		        ForwardBeatEncoder.INSTANCE,
		        ForwardDataEncoder.INSTANCE,
		        ForwardDisconnectEncoder.INSTANCE,
		        ForwardBeatEncoder.INSTANCE);
		
		ch.pipeline().addLast(
                new LoggingHandler(LogLevel.DEBUG),
                new TcpForwardServerDecoder(),
                ForwardLoginHandler.INSTANCE);
		
		ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(20, 10, 0));
        ch.pipeline().addLast(ForwardBeatHandler.INSTANCE);
	}

}
