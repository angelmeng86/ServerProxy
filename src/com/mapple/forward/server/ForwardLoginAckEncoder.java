package com.mapple.forward.server;

import com.mapple.forward.ForwardLoginAck;
import com.mapple.forward.ForwardUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class ForwardLoginAckEncoder extends MessageToByteEncoder<ForwardLoginAck> {

    public static final ForwardLoginAckEncoder INSTANCE = new ForwardLoginAckEncoder();
    
    @Override
    protected void encode(ChannelHandlerContext ctx, ForwardLoginAck msg, ByteBuf out)
            throws Exception {
        ForwardUtils.writeHeader(msg, out);
        out.writeByte(msg.getRep());
    }

}
