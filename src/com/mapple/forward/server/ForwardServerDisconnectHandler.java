package com.mapple.forward.server;

import com.mapple.forward.ForwardDisconnect;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ForwardServerDisconnectHandler extends SimpleChannelInboundHandler<ForwardDisconnect> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ForwardDisconnect msg) throws Exception {

    }

}
