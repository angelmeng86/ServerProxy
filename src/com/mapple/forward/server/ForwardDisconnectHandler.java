package com.mapple.forward.server;

import com.mapple.forward.ForwardDisconnect;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ForwardDisconnectHandler extends SimpleChannelInboundHandler<ForwardDisconnect> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ForwardDisconnect msg) throws Exception {

        
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
//        SocksServerUtils.closeOnFlush(ctx.channel());
    }
}
