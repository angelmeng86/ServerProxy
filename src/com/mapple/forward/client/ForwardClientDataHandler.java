package com.mapple.forward.client;

import com.mapple.forward.ForwardData;
import com.mapple.forward.ForwardDisconnect;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public class ForwardClientDataHandler extends SimpleChannelInboundHandler<ForwardData> {

    public static final ForwardClientDataHandler INSTANCE = new ForwardClientDataHandler();
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ForwardData msg) throws Exception {
        System.out.println("ForwardClientDataHandler:" + msg.getId());
        ConcurrentHashMap<String, Channel> connectList = ForwardConnectHandler.INSTANCE.connectList();
        Channel ch = connectList.get(msg.getId());
        if(ch == null) {
            ctx.channel().writeAndFlush(new ForwardDisconnect(msg));
//            msg.getData().release();
        }
        else {
            System.out.println("ForwardClientDataHandler " + ch);
            ch.writeAndFlush(msg.getData());
        }
    }

}
