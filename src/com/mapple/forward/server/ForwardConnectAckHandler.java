package com.mapple.forward.server;

import com.mapple.forward.ForwardConnectAck;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ForwardConnectAckHandler extends SimpleChannelInboundHandler<ForwardConnectAck> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ForwardConnectAck msg) throws Exception {
        
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
