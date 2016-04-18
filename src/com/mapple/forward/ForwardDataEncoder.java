package com.mapple.forward;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ForwardDataEncoder extends MessageToByteEncoder<ForwardData> {
    
    public static final ForwardDataEncoder INSTANCE = new ForwardDataEncoder();

    @Override
    protected void encode(ChannelHandlerContext ctx, ForwardData msg, ByteBuf out) throws Exception {
        ForwardUtils.writeHeader(msg, out);
        out.writeBytes(msg.getUid());
        out.writeShort(msg.getData().readableBytes());
        out.writeBytes(msg.getData());
        msg.getData().release();
    }

}
