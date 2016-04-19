package com.mapple.forward.client;

import com.mapple.forward.ForwardConnectAck;
import com.mapple.forward.ForwardUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressEncoder;
import io.netty.util.NetUtil;

@ChannelHandler.Sharable
public class ForwardConnectAckEncoder extends MessageToByteEncoder<ForwardConnectAck> {

private final Socks5AddressEncoder addressEncoder; 
    
    public static final ForwardConnectAckEncoder INSTANCE = new ForwardConnectAckEncoder();
    
    public ForwardConnectAckEncoder() {
        this.addressEncoder = Socks5AddressEncoder.DEFAULT;
    }
    
    @Override
    protected void encode(ChannelHandlerContext ctx, ForwardConnectAck msg, ByteBuf out)
            throws Exception {
        ForwardUtils.writeHeader(msg, out);
        out.writeBytes(NetUtil.createByteArrayFromIpAddressString(msg.getSrcAddr()));
        out.writeShort(msg.getSrcPort());
        out.writeByte(msg.getRep());
        out.writeByte(msg.dstAddrType().byteValue());
        addressEncoder.encodeAddress(msg.dstAddrType(), msg.dstAddr(), out);
        out.writeShort(msg.dstPort());
    }

}
