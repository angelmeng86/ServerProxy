package com.mapple.forward.client;

import com.mapple.forward.ForwardDisconnect;
import com.mapple.forward.ForwardUtils;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ForwardClientDisconnectHandler extends SimpleChannelInboundHandler<ForwardDisconnect> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ForwardDisconnect msg) throws Exception {
        ConcurrentHashMap<String, Channel> connectList = ForwardConnectHandler.INSTANCE.connectList();
        Channel ch = connectList.get(msg.getId());
        if(ch != null) {
            ForwardUtils.closeOnFlush(ch);
        }
        
    }

}
