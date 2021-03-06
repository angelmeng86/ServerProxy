package com.mapple.forward;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ForwardBeatEncoder extends MessageToByteEncoder<ForwardBeat> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ForwardBeat msg, ByteBuf out) throws Exception {
        ForwardUtils.writeHeader(msg, out);
    }

}
