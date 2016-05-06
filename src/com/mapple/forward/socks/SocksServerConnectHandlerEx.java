/*
 * Copyright 2012 The Netty Project
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
package com.mapple.forward.socks;

import com.mapple.forward.ForwardConnect;
import com.mapple.forward.ForwardLogin;
import com.mapple.forward.server.ForwardLoginHandler;
import com.mapple.socksproxy.SocksServerUtils;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Random;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.AttributeKey;
import io.netty.util.internal.ConcurrentSet;

@ChannelHandler.Sharable
public final class SocksServerConnectHandlerEx extends SimpleChannelInboundHandler<SocksMessage> {

    public static final SocksServerConnectHandlerEx INSTANCE = new SocksServerConnectHandlerEx();
    
    private static final AttributeKey<ForwardLogin> Session = AttributeKey.valueOf("Session");
    
    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final SocksMessage message) throws Exception {
        ConcurrentSet<Channel> proxyList = ForwardLoginHandler.INSTANCE.proxyList();
        String id = null;
        Channel channel = null;
        if (message instanceof Socks4CommandRequest) {
            final Socks4CommandRequest request = (Socks4CommandRequest) message;
            id = request.userId();
        } else if (message instanceof Socks5CommandRequest) {
            AttributeKey<String> SOCKS5 = AttributeKey.valueOf("socks5");
            id = ctx.channel().attr(SOCKS5).get();
        }
        if(id != null) {
//            System.out.println("id:" + id + " size:" + proxyList.size());
//            id = "Mapple";
            if (id.equals("Mapple") && proxyList.size() > 0) {
                int pos = new Random().nextInt() % proxyList.size();
                int i = 0;
                
                Iterator<Channel> it = proxyList.iterator();
                while(it.hasNext()) {
                    Channel ch = it.next();
                    if(pos == i++) {
                        channel = ch;
                        break;
                    }
                }
            }
            else {
                Iterator<Channel> it = proxyList.iterator();
                while(it.hasNext()) {
                    Channel ch = it.next();
                    ForwardLogin p = ch.attr(Session).get();
                    if (p.getProvince2() != null && id.toUpperCase().equals(p.getProvince2().toUpperCase())) {
                        channel = ch;
                        break;
                    }
                    if(id.equals(p.getRemoteAddr())) {
                        channel = ch;
                        break;
                    }
                }
            }
        }
        
        if (channel == null || !channel.isActive()) {
            if (message instanceof Socks4CommandRequest) {
                ctx.writeAndFlush(
                        new DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED));
                SocksServerUtils.closeOnFlush(ctx.channel());
            }
            else if (message instanceof Socks5CommandRequest) {
                final Socks5CommandRequest request = (Socks5CommandRequest) message;
                ctx.writeAndFlush(new DefaultSocks5CommandResponse(
                        Socks5CommandStatus.FAILURE, request.dstAddrType()));
                SocksServerUtils.closeOnFlush(ctx.channel());
            }
            else {
                ctx.close();
            }
            return;
        }
        if (message instanceof Socks4CommandRequest) {
//            System.out.println("SocksServerConnectHandlerEx Socks4CommandRequest");
            final Socks4CommandRequest request = (Socks4CommandRequest) message;
            InetSocketAddress addr = (InetSocketAddress) ctx.channel().remoteAddress();
            ForwardConnect fc = new ForwardConnect(addr.getAddress().getHostAddress(), addr.getPort(), Socks5AddressType.DOMAIN, request.dstAddr(), request.dstPort());
            fc.setSrcChannel(ctx.channel());
            ctx.channel().attr(AttributeKey.valueOf("socks4"));
            channel.writeAndFlush(fc);
            
            System.out.println("socks4:" + request.userId());
        } else if (message instanceof Socks5CommandRequest) {
            System.out.println("SocksServerConnectHandlerEx Socks5CommandRequest");
            final Socks5CommandRequest request = (Socks5CommandRequest) message;
            InetSocketAddress addr = (InetSocketAddress) ctx.channel().remoteAddress();
            ForwardConnect fc = new ForwardConnect(addr.getAddress().getHostAddress(), addr.getPort(), request.dstAddrType(), request.dstAddr(), request.dstPort());
            fc.setSrcChannel(ctx.channel());
            channel.writeAndFlush(fc);
        } else {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        SocksServerUtils.closeOnFlush(ctx.channel());
    }
}
