package com.mapple.forward;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.NetUtil;

public class ForwardDataEncoder extends MessageToByteEncoder<ForwardData> {
    
    @Override
    protected void encode(ChannelHandlerContext ctx, ForwardData msg, ByteBuf out) throws Exception {
        ForwardUtils.writeHeader(msg, out);
        out.writeBytes(NetUtil.createByteArrayFromIpAddressString(msg.getSrcAddr()));
        out.writeShort(msg.getSrcPort());
        
        out.writeShort(msg.getData().readableBytes());
        out.writeBytes(msg.getData());
        msg.getData().release();
    }

}
