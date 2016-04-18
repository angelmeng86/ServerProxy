package com.mapple.forward;

import io.netty.buffer.ByteBuf;

public class ForwardUtils {
    public static void writeHeader(ForwardMessage msg, ByteBuf out) {
        out.writeByte(msg.version().byteValue());
        out.writeByte(msg.cmd().byteValue());
        out.writeByte(0x00);
    }
}
