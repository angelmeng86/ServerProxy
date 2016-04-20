package com.mapple.forward.server;

import com.mapple.forward.ForwardDisconnect;
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
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

@ChannelHandler.Sharable
public class ForwardLoginHandler extends SimpleChannelInboundHandler<ForwardLogin> {
    
    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(ForwardLoginHandler.class);
    
    public static final ForwardLoginHandler INSTANCE = new ForwardLoginHandler();
    
    private ConcurrentHashMap<String, ForwardLogin> proxyList = new ConcurrentHashMap<String, ForwardLogin>();

    public ConcurrentHashMap<String, ForwardLogin> proxyList() {
        return proxyList;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ForwardLogin msg)
            throws Exception {
        InetSocketAddress addr = (InetSocketAddress) ctx.channel().remoteAddress();
        msg.setRemoteAddr(addr.getAddress().getHostAddress());
        msg.setRemotePort(addr.getPort());
        msg.setRemoteChannel(ctx.channel());
        if(proxyList.containsKey(msg.getUserName())) {
            ctx.writeAndFlush(new ForwardLoginAck((byte)0x01));
            ForwardUtils.closeOnFlush(ctx.channel());
        }
        else {
            ctx.writeAndFlush(new ForwardLoginAck((byte)0x00));
            logger.info("客户端转发连接：" + msg.getUserName() + "[" + msg.getRemoteAddr() + ":" + msg.getRemotePort() + "]");
            proxyList.put(ctx.channel().id().asLongText(), msg);
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
                    proxyList.remove(future.channel().id().asLongText());
                }
            });
        }
        
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        ForwardUtils.closeOnFlush(ctx.channel());
    }
}
