package com.mapple.forward.server;

import com.mapple.forward.ForwardConnectAck;
import com.mapple.forward.ForwardDisconnect;
import com.mapple.forward.ForwardDataRemoteHandler;
import com.mapple.forward.socks.SocksServerConnectHandlerEx;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;

public class ForwardConnectAckHandler extends SimpleChannelInboundHandler<ForwardConnectAck> {

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final ForwardConnectAck msg) throws Exception {
        ForwardConnectEncoder ce = (ForwardConnectEncoder)ctx.pipeline().get("connectEncoder");
        ConcurrentHashMap<String, Channel> connectList = ce.connectList();
        final Channel ch = connectList.get(msg.getId());
        if(ch == null) {
            ctx.writeAndFlush(new ForwardDisconnect(msg));
        }
        else {
            //Socks4未处理
            ChannelFuture responseFuture = ch.writeAndFlush(new DefaultSocks5CommandResponse(
                    Socks5CommandStatus.SUCCESS, msg.dstAddrType()));
            responseFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) {
                    ch.pipeline().remove(SocksServerConnectHandlerEx.class);
                    ch.pipeline().addLast(new ForwardDataRemoteHandler(ctx.channel(), msg));
                }
            });
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
//        SocksServerUtils.closeOnFlush(ctx.channel());
    }

}
