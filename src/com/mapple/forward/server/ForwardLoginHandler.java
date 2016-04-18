package com.mapple.forward.server;

import com.mapple.forward.ForwardLogin;
import com.mapple.forward.ForwardLoginAck;
import com.mapple.socksproxy.SocksServerUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class ForwardLoginHandler extends SimpleChannelInboundHandler<ForwardLogin> {
    
    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(ForwardLoginHandler.class);
    
    public static final ForwardLoginHandler INSTANCE = new ForwardLoginHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ForwardLogin msg)
            throws Exception {
        
        System.out.println("客户端转发连接：" + msg.getUserName() + " " + ctx);
        ctx.channel().writeAndFlush(new ForwardLoginAck((byte)0x00));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        SocksServerUtils.closeOnFlush(ctx.channel());
    }
}
