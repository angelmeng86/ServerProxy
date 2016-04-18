package com.mapple.forward;

public class ForwardDisconnect extends ForwardAddrMessage {

    public ForwardDisconnect(byte[] uid, String srcAddr, int srcPort) {
        super(uid, srcAddr, srcPort);
    }

    @Override
    public ForwardCmd cmd() {
        return ForwardCmd.DISCONNECT;
    }

}
