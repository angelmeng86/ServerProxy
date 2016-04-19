package com.mapple.forward;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.NetUtil;

@ChannelHandler.Sharable
public class ForwardDataEncoder extends MessageToByteEncoder<ForwardData> {
    
    public static final ForwardDataEncoder INSTANCE = new ForwardDataEncoder();

    @Override
    protected void encode(ChannelHandlerContext ctx, ForwardData msg, ByteBuf out) throws Exception {
        ForwardUtils.writeHeader(msg, out);
        out.writeBytes(NetUtil.createByteArrayFromIpAddressString(msg.getSrcAddr()));
        out.writeShort(msg.getSrcPort());
        
        System.out.println("ForwardDataEncoder " + msg.getId() + " " + msg.getData().length + " " + ctx.channel());
        
        out.writeByte(msg.getData().length);
        out.writeBytes(msg.getData());
//        msg.getData().release();
    }

}
