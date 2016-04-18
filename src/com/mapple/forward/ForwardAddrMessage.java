package com.mapple.forward;

public abstract class ForwardAddrMessage implements ForwardMessage {
    
    private final byte[] uid;
    private final String srcAddr;
    private final int srcPort;
    
    public ForwardAddrMessage(byte[] uid, String srcAddr, int srcPort) {
        if (uid == null) {
            throw new NullPointerException("uid");
        }
        if (srcAddr == null) {
            throw new NullPointerException("srcAddr");
        }
        
        if (srcPort <= 0 || srcPort >= 65536) {
            throw new IllegalArgumentException("srcPort: " + srcPort + " (expected: 1~65535)");
        }
        
        this.uid = uid;
        this.srcAddr = srcAddr;
        this.srcPort = srcPort;
    }

	@Override
	public ForwardVersion version() {
		return ForwardVersion.FORWARD1;
	}

    public String getSrcAddr() {
        return srcAddr;
    }

    public int getSrcPort() {
        return srcPort;
    }

    public byte[] getUid() {
        return uid;
    }

}
