package com.mapple.forward.server;

import com.mapple.forward.ForwardBeat;
import com.mapple.forward.ForwardUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class ForwardBeatEncoder extends MessageToByteEncoder<ForwardBeat> {
    
    public static final ForwardBeatEncoder INSTANCE = new ForwardBeatEncoder();

    @Override
    protected void encode(ChannelHandlerContext ctx, ForwardBeat msg, ByteBuf out) throws Exception {
        ForwardUtils.writeHeader(msg, out);
    }

}
