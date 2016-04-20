package com.mapple.forward;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.NetUtil;

public class ForwardDisconnectEncoder extends MessageToByteEncoder<ForwardDisconnect> {
    
    @Override
    protected void encode(ChannelHandlerContext ctx, ForwardDisconnect msg, ByteBuf out)
            throws Exception {
        ForwardUtils.writeHeader(msg, out);
        out.writeBytes(NetUtil.createByteArrayFromIpAddressString(msg.getSrcAddr()));
        out.writeShort(msg.getSrcPort());
    }

}
