package com.mapple.forward.client;

import com.mapple.forward.ForwardDisconnect;
import com.mapple.socksproxy.SocksServerUtils;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public class ForwardClientDisconnectHandler extends SimpleChannelInboundHandler<ForwardDisconnect> {

    public static final ForwardClientDisconnectHandler INSTANCE = new ForwardClientDisconnectHandler();
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ForwardDisconnect msg) throws Exception {
        ConcurrentHashMap<String, Channel> connectList = ForwardConnectHandler.INSTANCE.connectList();
        Channel ch = connectList.get(msg.getId());
        if(ch != null) {
            SocksServerUtils.closeOnFlush(ch);
        }
        
    }

}
