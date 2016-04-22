/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mapple.http;

import com.mapple.forward.ForwardForceClose;
import com.mapple.forward.ForwardLogin;
import com.mapple.forward.server.ForwardLoginHandler;

import java.util.Iterator;
import java.util.List;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AsciiString;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.ConcurrentSet;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class HttpHelloWorldServerHandler extends ChannelInboundHandlerAdapter {
    private static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");
    private static final AsciiString CONTENT_LENGTH = new AsciiString("Content-Length");
//    private static final AsciiString CONNECTION = new AsciiString("Connection");
//    private static final AsciiString KEEP_ALIVE = new AsciiString("keep-alive");

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            
            

            if (HttpUtil.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }
            
            QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
            List<String> close = decoder.parameters().get("close");
            int pos = -1;
            if(close != null && close.size() > 0) {
            	pos = Integer.valueOf(close.get(0));
            }
            
            StringBuilder body = new StringBuilder();
            ConcurrentSet<Channel> proxyList = ForwardLoginHandler.INSTANCE.proxyList();
            int i = 0;
            AttributeKey<ForwardLogin> Session = AttributeKey.valueOf("Session");
            Iterator<Channel> it = proxyList.iterator();
            while(it.hasNext()) {
                Channel ch = it.next();
                ForwardLogin p = ch.attr(Session).get();
                body.append(i + 1);
                body.append("  ");
                body.append(p.getProvince() + p.getCity() == null? "" : p.getCity() + "[" + p.getProvince2() + "]");
                body.append("  ");
                body.append(p.getRemoteAddr() + ":" + p.getRemotePort());
                body.append("  ");
                body.append(p.getCarrier());
                if(i++ == pos) {
                    body.append("  [CLOSED]");
                    ch.writeAndFlush(new ForwardForceClose());
                }
                body.append("\n");
            }
            String data = body.toString();
            if(data.isEmpty()) {
            	data = "木有数据哇";
            }
            
//            boolean keepAlive = HttpUtil.isKeepAlive(req);
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(data.getBytes(CharsetUtil.UTF_8)));
            response.headers().set(CONTENT_TYPE, "text/plain; charset=utf-8");
            response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
            
            
//            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
//            } else {
//                response.headers().set(CONNECTION, KEEP_ALIVE);
//                ctx.write(response);
//            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
