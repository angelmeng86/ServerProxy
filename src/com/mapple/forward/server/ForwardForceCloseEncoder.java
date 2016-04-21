package com.mapple.forward.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import com.mapple.forward.ForwardForceClose;
import com.mapple.forward.ForwardUtils;

public class ForwardForceCloseEncoder extends MessageToByteEncoder<ForwardForceClose> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ForwardForceClose msg, ByteBuf out)
            throws Exception {
        ForwardUtils.writeHeader(msg, out);
    }

}
