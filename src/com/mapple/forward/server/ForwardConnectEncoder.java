package com.mapple.forward.server;

import com.mapple.forward.ForwardConnect;
import com.mapple.forward.ForwardDisconnect;
import com.mapple.forward.ForwardUtils;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressEncoder;
import io.netty.util.NetUtil;

public class ForwardConnectEncoder extends MessageToByteEncoder<ForwardConnect> {
    
    private final Socks5AddressEncoder addressEncoder; 
    
    private ConcurrentHashMap<String, Channel> connectList = new ConcurrentHashMap<String, Channel>();
    
    public ForwardConnectEncoder() {
        this.addressEncoder = Socks5AddressEncoder.DEFAULT;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final ForwardConnect msg, ByteBuf out)
            throws Exception {
        ForwardUtils.writeHeader(msg, out);
        out.writeBytes(NetUtil.createByteArrayFromIpAddressString(msg.getSrcAddr()));
        out.writeShort(msg.getSrcPort());
        out.writeByte(msg.dstAddrType().byteValue());
        addressEncoder.encodeAddress(msg.dstAddrType(), msg.dstAddr(), out);
        out.writeShort(msg.dstPort());
        System.out.println("服务端总连接数：" + connectList.size());
        connectList.put(msg.getId(), msg.getSrcChannel());
        msg.getSrcChannel().closeFuture()
        .addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future)
                    throws Exception {
                System.out.println("服务端连接断开：" + msg.getId());
                if(ctx.channel().isActive()) {
                    ctx.writeAndFlush(new ForwardDisconnect(msg));
                }
                if(connectList.containsKey(msg.getId())) {
                    connectList.remove(msg.getId());
                }
                System.out.println("服务端剩余连接数：" + connectList.size());
            }
        });
    }

    public ConcurrentHashMap<String, Channel> connectList() {
        return connectList;
    }

}
