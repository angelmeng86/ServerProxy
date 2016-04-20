package com.mapple.forward;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

public class ForwardUtils {
    public static void writeHeader(ForwardMessage msg, ByteBuf out) {
        out.writeByte(msg.version().byteValue());
        out.writeByte(msg.cmd().byteValue());
        out.writeByte(0x00);
    }
    
    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    public static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
