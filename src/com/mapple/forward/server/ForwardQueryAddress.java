package com.mapple.forward.server;

import com.mapple.forward.ForwardLogin;
import com.mapple.forward.ForwardUtils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.URI;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class ForwardQueryAddress {
    
    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(ForwardQueryAddress.class);
    
    private static final AttributeKey<ForwardLogin> Session = AttributeKey.valueOf("Session");
    
    public static boolean hasAddress(Channel ch) {
        ForwardLogin msg = ch.attr(Session).get();
        if(msg.getProvince() == null || msg.getProvince().isEmpty()) {
            return false;
        }
        return true;
    }
    
    public static void queryAddress(final Channel ch) {
        
        final ForwardLogin msg = ch.attr(Session).get();
        
        Bootstrap b = new Bootstrap();
            b.group(ch.eventLoop())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_REUSEADDR, true);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                // 客户端发送的是httprequest，所以要使用HttpRequestEncoder进行编码
                ch.pipeline().addLast(new HttpRequestEncoder());
                // 客户端接收到的是httpResponse响应，所以要使用HttpResponseDecoder进行解码
                ch.pipeline().addLast(new HttpResponseDecoder());
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object request) throws Exception {
                        if(request instanceof HttpContent)
                        {
                            HttpContent content = (HttpContent)request;
                            ByteBuf buf = content.content();
                            String json = buf.toString(io.netty.util.CharsetUtil.UTF_8);
                            if(json.trim().isEmpty()) {
                                buf.release();
                                return;
                            }
//                            System.out.println(json);
                            JSONParser parser = new JSONParser();
                            JSONObject obj = (JSONObject)parser.parse(json);
                            if(Integer.valueOf(obj.get("errNum").toString()) == 0) {
                                JSONObject retData = (JSONObject)obj.get("retData");
                                msg.setProvince(retData.get("province").toString());
                                logger.info("客户端地址：" + msg.getUserName() + "[" + msg.getRemoteAddr() + ":" + msg.getRemotePort() + "] " + msg.getProvince());
                            }
                            buf.release();
                            ctx.close();
                        }
                    }
                    
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
                        throwable.printStackTrace();
                        ctx.close();
                    }
                });
            }
        });
        //http://apis.baidu.com/apistore/iplookupservice/iplookup?ip=
        b.connect("apis.baidu.com", 80).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    // Connection established use handler provided results
//                    URI uri = new URI("http://apis.baidu.com/apistore/iplookupservice/iplookup?ip=" + msg.getRemoteAddr());
                    URI uri = new URI("http://apis.baidu.com/apistore/iplookupservice/iplookup?ip=120.194.4.141");
                    DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                            uri.toASCIIString());

                    // 构建http请求
                    request.headers().set(HttpHeaderNames.HOST, "apis.baidu.com");
                    request.headers().set("apikey", "ce45a88f920769bda3a789876300b6c4");
                    // 发送http请求
                    future.channel().writeAndFlush(request);
                } else {
                    // Close the connection if the connection attempt has failed.
                    ForwardUtils.closeOnFlush(ch);
                }
            }
        });
    }
}
