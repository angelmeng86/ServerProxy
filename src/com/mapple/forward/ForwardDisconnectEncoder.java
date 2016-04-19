package com.mapple.forward;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.NetUtil;

@ChannelHandler.Sharable
public class ForwardDisconnectEncoder extends MessageToByteEncoder<ForwardDisconnect> {
    
    public static final ForwardDisconnectEncoder INSTANCE = new ForwardDisconnectEncoder();

    @Override
    protected void encode(ChannelHandlerContext ctx, ForwardDisconnect msg, ByteBuf out)
            throws Exception {
        ForwardUtils.writeHeader(msg, out);
        out.writeBytes(NetUtil.createByteArrayFromIpAddressString(msg.getSrcAddr()));
        out.writeShort(msg.getSrcPort());
    }

}
