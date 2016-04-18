package com.mapple.forward;

import io.netty.buffer.ByteBuf;

public class ForwardData extends ForwardAddrMessage {
    
    private final ByteBuf data;

    public ForwardData(byte[] uid, String srcAddr, int srcPort, ByteBuf data) {
        super(uid, srcAddr, srcPort);
        if(data == null) {
            throw new NullPointerException("data");
        }
        this.data = data;
    }

    @Override
    public ForwardCmd cmd() {
        return ForwardCmd.DATA;
    }

    public ByteBuf getData() {
        return data;
    }

}
