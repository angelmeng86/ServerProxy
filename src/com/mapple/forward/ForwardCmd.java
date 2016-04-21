package com.mapple.forward;

public enum ForwardCmd {
	LOGIN((byte) 0x01),
	LOGINACK((byte) 0x11),
	BEAT((byte) 0x31),
	CONNECT((byte) 0x13),
	CONNECTACK((byte) 0x03),
	DATA((byte) 0x30),
	DISCONNECT((byte) 0x32),
	FORCECLOSE((byte) 0x14),

    UNKNOWN((byte) 0xff);

    public static ForwardCmd valueOf(byte b) {
        if (b == LOGIN.byteValue()) {
            return LOGIN;
        }
        if (b == LOGINACK.byteValue()) {
            return LOGINACK;
        }
        if (b == BEAT.byteValue()) {
            return BEAT;
        }
        if (b == CONNECT.byteValue()) {
            return CONNECT;
        }
        if (b == CONNECTACK.byteValue()) {
            return CONNECTACK;
        }
        if (b == DATA.byteValue()) {
            return DATA;
        }
        if (b == DISCONNECT.byteValue()) {
            return DISCONNECT;
        }
        if (b == FORCECLOSE.byteValue()) {
            return FORCECLOSE;
        }
        return UNKNOWN;
    }

    private final byte b;

    ForwardCmd(byte b) {
        this.b = b;
    }

    public byte byteValue() {
        return b;
    }
}
