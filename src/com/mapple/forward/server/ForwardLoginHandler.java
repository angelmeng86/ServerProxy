package com.mapple.forward.server;

import com.mapple.forward.ForwardLogin;
import com.mapple.forward.ForwardLoginAck;
import com.mapple.forward.ForwardUtils;


import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import io.netty.util.internal.ConcurrentSet;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

@ChannelHandler.Sharable
public class ForwardLoginHandler extends SimpleChannelInboundHandler<ForwardLogin> {
    
    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(ForwardLoginHandler.class);
    
    public static final ForwardLoginHandler INSTANCE = new ForwardLoginHandler();
    
    private ConcurrentSet<Channel> proxyList = new ConcurrentSet<Channel>();
    
    public ConcurrentSet<Channel> proxyList() {
        return proxyList;
    }
    
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final ForwardLogin msg)
            throws Exception {
        InetSocketAddress addr = (InetSocketAddress) ctx.channel().remoteAddress();
        msg.setRemoteAddr(addr.getAddress().getHostAddress());
        msg.setRemotePort(addr.getPort());
        if(proxyList.contains(ctx.channel())) {
//            ctx.writeAndFlush(new ForwardLoginAck((byte)0x01));
//            ForwardUtils.closeOnFlush(ctx.channel());
        }
        else {
            ctx.writeAndFlush(new ForwardLoginAck((byte)0x00));
            logger.info("客户端转发连接：" + msg.getUserName() + "[" + msg.getRemoteAddr() + ":" + msg.getRemotePort() + "]");
            
            AttributeKey<ForwardLogin> Session = AttributeKey.valueOf("Session");
            ctx.channel().attr(Session).set(msg);
            proxyList.add(ctx.channel());
             
            ctx.channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future)
                        throws Exception {
                    ForwardConnectEncoder ce = (ForwardConnectEncoder)future.channel().pipeline().get("connectEncoder");
                    ConcurrentHashMap<String, Channel> connectList = ce.connectList();
                    for(Channel ch : connectList.values()) {
                        ForwardUtils.closeOnFlush(ch);
                    }
                    connectList.clear();
                    proxyList.remove(future.channel());
                }
            });
            ForwardQueryAddress.queryAddress(ctx.channel());
        }
            
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        ForwardUtils.closeOnFlush(ctx.channel());
    }
}
