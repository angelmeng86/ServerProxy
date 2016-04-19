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
import java.util.concurrent.ConcurrentHashMap;

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

@ChannelHandler.Sharable
public final class SocksServerConnectHandlerEx extends SimpleChannelInboundHandler<SocksMessage> {

    public static final SocksServerConnectHandlerEx INSTANCE = new SocksServerConnectHandlerEx();
    
    private boolean oye = false;
    
    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final SocksMessage message) throws Exception {
        ConcurrentHashMap<String, ForwardLogin> proxyList = ForwardLoginHandler.INSTANCE.proxyList();
        ForwardLogin proxy = proxyList.get("LWZ");
        System.out.println("SocksServerConnectHandlerEx");
        if (proxy == null || !proxy.getRemoteChannel().isActive()) {
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
        }
        if(oye) {
            ctx.close();
            return;
        }
        oye = true;
        if (message instanceof Socks4CommandRequest) {
            System.out.println("SocksServerConnectHandlerEx Socks4CommandRequest");
            final Socks4CommandRequest request = (Socks4CommandRequest) message;
            InetSocketAddress addr = (InetSocketAddress) ctx.channel().remoteAddress();
            ForwardConnect fc = new ForwardConnect(addr.getAddress().getHostAddress(), addr.getPort(), Socks5AddressType.DOMAIN, request.dstAddr(), request.dstPort());
            fc.setSrcChannel(ctx.channel());
            proxy.getRemoteChannel().writeAndFlush(fc);
        } else if (message instanceof Socks5CommandRequest) {
            System.out.println("SocksServerConnectHandlerEx Socks5CommandRequest");
            final Socks5CommandRequest request = (Socks5CommandRequest) message;
            InetSocketAddress addr = (InetSocketAddress) ctx.channel().remoteAddress();
            ForwardConnect fc = new ForwardConnect(addr.getAddress().getHostAddress(), addr.getPort(), request.dstAddrType(), request.dstAddr(), request.dstPort());
            fc.setSrcChannel(ctx.channel());
            proxy.getRemoteChannel().writeAndFlush(fc);
        } else {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SocksServerUtils.closeOnFlush(ctx.channel());
    }
}
