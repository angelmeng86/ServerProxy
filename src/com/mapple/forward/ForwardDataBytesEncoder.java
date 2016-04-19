package com.mapple.forward;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class ForwardDataBytesEncoder extends MessageToByteEncoder<byte[]> {

    
    public static final ForwardDataBytesEncoder INSTANCE = new ForwardDataBytesEncoder();
    
    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf out) throws Exception {
        
        System.out.println("ForwardDataBytesEncoder " + msg.length + " " + ctx);
        out.writeBytes(msg);
    }

}
