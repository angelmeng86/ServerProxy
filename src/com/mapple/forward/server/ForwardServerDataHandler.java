package com.mapple.forward.server;

import com.mapple.forward.ForwardData;
import com.mapple.forward.ForwardDisconnect;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public class ForwardServerDataHandler extends SimpleChannelInboundHandler<ForwardData> {

    public static final ForwardServerDataHandler INSTANCE = new ForwardServerDataHandler();
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ForwardData msg) throws Exception {
        System.out.println("ForwardServerDataHandler write len " + msg.getData().length);
        ForwardConnectEncoder ce = (ForwardConnectEncoder)ctx.pipeline().get("connectEncoder");
        ConcurrentHashMap<String, Channel> connectList = ce.connectList();
        Channel ch = connectList.get(msg.getId());
        if(ch == null) {
            ctx.writeAndFlush(new ForwardDisconnect(msg));
        }
        else {
            ch.writeAndFlush(msg.getData());
        }
        
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
