package com.mapple.forward;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ForwardDisconnectEncoder extends MessageToByteEncoder<ForwardDisconnect> {
    
    public static final ForwardDisconnectEncoder INSTANCE = new ForwardDisconnectEncoder();

    @Override
    protected void encode(ChannelHandlerContext ctx, ForwardDisconnect msg, ByteBuf out)
            throws Exception {
        ForwardUtils.writeHeader(msg, out);
        out.writeBytes(msg.getUid());
    }

}
