package com.mapple.forward;

public abstract class ForwardAddrMessage implements ForwardMessage {
    
    private final String id;
    private final String srcAddr;
    private final int srcPort;
    
    public ForwardAddrMessage(ForwardAddrMessage addr) {
        this(addr.srcAddr, addr.srcPort);
    }
    
    public ForwardAddrMessage(String srcAddr, int srcPort) {
        if (srcAddr == null) {
            throw new NullPointerException("srcAddr");
        }
        
        if (srcPort <= 0 || srcPort >= 65536) {
            throw new IllegalArgumentException("srcPort: " + srcPort + " (expected: 1~65535)");
        }
        
        this.id = srcAddr + ":" + srcPort;
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

    public String getId() {
        return id;
    }

}
