package com.mapple.forward;

public class ForwardDisconnect extends ForwardAddrMessage {
    
    public ForwardDisconnect(ForwardAddrMessage addr) {
        super(addr);
    }

    public ForwardDisconnect(String srcAddr, int srcPort) {
        super(srcAddr, srcPort);
    }

    @Override
    public ForwardCmd cmd() {
        return ForwardCmd.DISCONNECT;
    }

}
