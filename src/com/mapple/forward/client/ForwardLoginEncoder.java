package com.mapple.forward.client;

import com.mapple.forward.ForwardLogin;
import com.mapple.forward.ForwardUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;

public class ForwardLoginEncoder extends MessageToByteEncoder<ForwardLogin> {

    public static final ForwardLoginEncoder INSTANCE = new ForwardLoginEncoder();
    
    @Override
    protected void encode(ChannelHandlerContext ctx, ForwardLogin msg, ByteBuf out)
            throws Exception {
        System.out.println("发送登录请求");
        ForwardUtils.writeHeader(msg, out);
        byte[] name = msg.getUserName().getBytes(CharsetUtil.UTF_8);
        out.writeByte(name.length);
        out.writeBytes(name);
    }

}