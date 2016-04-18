package com.mapple.forward.server;

import com.mapple.forward.ForwardConnect;
import com.mapple.forward.ForwardUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressEncoder;

@ChannelHandler.Sharable
public class ForwardConnectEncoder extends MessageToByteEncoder<ForwardConnect> {
    
    private final Socks5AddressEncoder addressEncoder; 
    
    public static final ForwardConnectEncoder INSTANCE = new ForwardConnectEncoder();
    
    public ForwardConnectEncoder() {
        this.addressEncoder = Socks5AddressEncoder.DEFAULT;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ForwardConnect msg, ByteBuf out)
            throws Exception {
        ForwardUtils.writeHeader(msg, out);
        out.writeBytes(msg.getUid());
        out.writeByte(msg.dstAddrType().byteValue());
        addressEncoder.encodeAddress(msg.dstAddrType(), msg.dstAddr(), out);
        out.writeShort(msg.dstPort());
    }

}
