package com.mapple.forward;

import io.netty.buffer.ByteBuf;

public class ForwardData extends ForwardAddrMessage {
    
//    private final ByteBuf data;
    private final byte[] buffer;
    
    public ForwardData(ForwardAddrMessage addr, ByteBuf data) {
        super(addr);
        if(data == null) {
            throw new NullPointerException("data");
        }
//        this.data = data;
        buffer = new byte[data.readableBytes()];
        data.getBytes(data.readerIndex(), buffer);
    }

    public ForwardData(String srcAddr, int srcPort, ByteBuf data) {
        super(srcAddr, srcPort);
        if(data == null) {
            throw new NullPointerException("data");
        }
//        this.data = data;
        buffer = new byte[data.readableBytes()];
        data.getBytes(data.readerIndex(), buffer);
    }

    @Override
    public ForwardCmd cmd() {
        return ForwardCmd.DATA;
    }

    public byte[] getData() {
        return buffer;
    }

}
