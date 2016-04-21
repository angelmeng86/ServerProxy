package com.mapple.forward.server;

import com.mapple.forward.ForwardBeatEncoder;
import com.mapple.forward.ForwardDataEncoder;
import com.mapple.forward.ForwardDisconnectEncoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class TcpForwardServerInitializer extends ChannelInitializer<SocketChannel> {

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast(
		        new ForwardLoginAckEncoder(),
		        new ForwardBeatEncoder(),
		        new ForwardDataEncoder(),
		        new ForwardDisconnectEncoder(),
		        new ForwardForceCloseEncoder());
		
		ch.pipeline().addLast("connectEncoder", new ForwardConnectEncoder());
		ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(60, 30, 0));
		
		ch.pipeline().addLast(
//				new LoggingHandler(LogLevel.DEBUG),
				new ForwardBeatHandler(),
				new TcpForwardServerDecoder(),
                ForwardLoginHandler.INSTANCE,
                new ForwardServerDataHandler(),
                new ForwardConnectAckHandler(),
                new ForwardServerDisconnectHandler());
	}

}
