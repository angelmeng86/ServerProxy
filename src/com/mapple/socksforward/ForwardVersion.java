package com.mapple.socksforward;

public enum ForwardVersion {
	FORWARD1((byte) 0x01),

    UNKNOWN((byte) 0xff);

    public static ForwardVersion valueOf(byte b) {
        if (b == FORWARD1.byteValue()) {
            return FORWARD1;
        }
        return UNKNOWN;
    }

    private final byte b;

    ForwardVersion(byte b) {
        this.b = b;
    }

    public byte byteValue() {
        return b;
    }
}
