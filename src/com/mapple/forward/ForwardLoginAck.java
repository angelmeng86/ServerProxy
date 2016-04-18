package com.mapple.forward;

import com.mapple.forward.ForwardCmd;
import com.mapple.forward.ForwardMessage;
import com.mapple.forward.ForwardVersion;

public class ForwardLoginAck implements ForwardMessage {

    private final byte rep;
    
    public ForwardLoginAck(byte rep) {
        this.rep = rep;
    }
    
    @Override
    public ForwardVersion version() {
        return ForwardVersion.FORWARD1;
    }

    @Override
    public ForwardCmd cmd() {
        return ForwardCmd.LOGINACK;
    }

    public byte getRep() {
        return rep;
    }

}
