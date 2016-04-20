package com.mapple.forward.server;

import com.mapple.forward.ForwardDisconnect;
import com.mapple.forward.ForwardUtils;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ForwardServerDisconnectHandler extends SimpleChannelInboundHandler<ForwardDisconnect> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ForwardDisconnect msg) throws Exception {
        ForwardConnectEncoder ce = (ForwardConnectEncoder)ctx.pipeline().get("connectEncoder");
        ConcurrentHashMap<String, Channel> connectList = ce.connectList();
        Channel ch = connectList.get(msg.getId());
        if(ch != null) {
            ForwardUtils.closeOnFlush(ch);
        }
    }

}
