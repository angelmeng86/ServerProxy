package com.mapple.forward.client;

import com.mapple.forward.ForwardConnect;
import com.mapple.forward.ForwardConnectAck;
import com.mapple.forward.ForwardDataBytesEncoder;
import com.mapple.forward.ForwardDisconnect;
import com.mapple.forward.server.TcpForwardServerDecoder;
import com.mapple.socksproxy.DirectClientHandler;
import com.mapple.socksproxy.RelayHandler;
import com.mapple.socksproxy.SocksServerUtils;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

@ChannelHandler.Sharable
public class ForwardConnectHandler extends SimpleChannelInboundHandler<ForwardConnect> {

    public static final ForwardConnectHandler INSTANCE = new ForwardConnectHandler();
    
    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(ForwardConnectHandler.class);
    
    private ConcurrentHashMap<String, Channel> connectList = new ConcurrentHashMap<String, Channel>();
    
//    private Bootstrap b = null;
    
    public ConcurrentHashMap<String, Channel> connectList() {
        return connectList;
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        
        for(Channel ch : connectList.values()) {
            SocksServerUtils.closeOnFlush(ch);
        }
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final ForwardConnect msg) throws Exception {
        System.out.println("ForwardConnectHandler " + msg.getId());
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(
                new FutureListener<Channel>() {
                    @Override
                    public void operationComplete(final Future<Channel> future) throws Exception {
                        final Channel outboundChannel = future.getNow();
                        if (future.isSuccess()) {
                            ChannelFuture responseFuture =
                                    ctx.channel().writeAndFlush(new ForwardConnectAck(msg, ForwardConnectAck.SUCCESS));

                            responseFuture.addListener(new ChannelFutureListener() {
                                @Override
                                public void operationComplete(ChannelFuture channelFuture) {
                                    outboundChannel.pipeline().addLast(ForwardDataBytesEncoder.INSTANCE);
                                    outboundChannel.pipeline().addLast(new ForwardDataRemoteHandler(ctx.channel(), msg));
                                    connectList.put(msg.getId(), outboundChannel);
                                    outboundChannel.closeFuture()
                                            .addListener(new ChannelFutureListener() {
                                                @Override
                                                public void operationComplete(ChannelFuture future)
                                                        throws Exception {
                                                    if(ctx.channel().isActive()) {
                                                        ctx.channel().writeAndFlush(new ForwardDisconnect(msg));
                                                    }
                                                    if(connectList.containsKey(msg.getId())) {
                                                        connectList.remove(msg.getId());
                                                    }
                                                }
                                            });
                                }
                            });
                        } else {
                            ctx.channel().writeAndFlush(new ForwardConnectAck(msg, ForwardConnectAck.FAILURE));
                        }
                    }
                });
        Bootstrap b = null;
        if(b == null) {
            b = new Bootstrap();
            b.group(ctx.channel().eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true);
        }
        b.handler(new DirectClientHandler(promise));
        b.connect(msg.dstAddr(), msg.dstPort()).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    // Connection established use handler provided results
                } else {
                    // Close the connection if the connection attempt has failed.
                    ctx.channel().writeAndFlush(
                            new ForwardConnectAck(msg, ForwardConnectAck.FAILURE));
                }
            }
        });
    }

}
