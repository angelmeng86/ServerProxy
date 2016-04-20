package com.mapple.forward.client;

import com.mapple.forward.ForwardBeatEncoder;
import com.mapple.forward.ForwardDataEncoder;
import com.mapple.forward.ForwardDisconnectEncoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class TcpForwardClientInitializer extends ChannelInitializer<SocketChannel> {

    private final String userName;
    
    public TcpForwardClientInitializer(String userName) {
        this.userName = userName;
    }
    
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
    	ch.pipeline().addLast(
    			new ForwardLoginEncoder(),
                new ForwardBeatEncoder(),
                new ForwardDataEncoder(),
                new ForwardConnectAckEncoder(),
                new ForwardDisconnectEncoder());
    	
        ch.pipeline().addLast(
//                new LoggingHandler(LogLevel.DEBUG),
                new TcpForwardClientDecoder(userName), 
                ForwardConnectHandler.INSTANCE,
                new ForwardClientDataHandler(),
                new ForwardClientDisconnectHandler());
        
    }

}
