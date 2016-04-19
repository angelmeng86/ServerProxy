package com.mapple.forward;

import com.mapple.forward.ForwardAddrMessage;
import com.mapple.forward.ForwardCmd;

import java.net.IDN;

import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.util.NetUtil;

public class ForwardConnectAck extends ForwardAddrMessage {
    
    private final Socks5AddressType dstAddrType;
    private final String dstAddr;
    private final int dstPort;
    private final byte rep;
    
    public static final byte SUCCESS = 0x00;
    public static final byte FAILURE = 0x01;

    @Override
    public ForwardCmd cmd() {
        return ForwardCmd.CONNECTACK;
    }

    public ForwardConnectAck(ForwardConnect connect, byte rep) {
        this(connect.getSrcAddr(), connect.getSrcPort(), rep, connect.dstAddrType(), connect.dstAddr(), connect.dstPort());
    }
    
    public ForwardConnectAck(String srcAddr, int srcPort, byte rep, Socks5AddressType dstAddrType, String dstAddr, int dstPort) {
        super(srcAddr, srcPort);
        
        if (dstAddrType == null) {
            throw new NullPointerException("dstAddrType");
        }
        if (dstAddr == null) {
            throw new NullPointerException("dstAddr");
        }

        if (dstAddrType == Socks5AddressType.IPv4) {
            if (!NetUtil.isValidIpV4Address(dstAddr)) {
                throw new IllegalArgumentException("dstAddr: " + dstAddr + " (expected: a valid IPv4 address)");
            }
        } else if (dstAddrType == Socks5AddressType.DOMAIN) {
            dstAddr = IDN.toASCII(dstAddr);
            if (dstAddr.length() > 255) {
                throw new IllegalArgumentException("dstAddr: " + dstAddr + " (expected: less than 256 chars)");
            }
        } else if (dstAddrType == Socks5AddressType.IPv6) {
            if (!NetUtil.isValidIpV6Address(dstAddr)) {
                throw new IllegalArgumentException("dstAddr: " + dstAddr + " (expected: a valid IPv6 address");
            }
        }

        if (dstPort <= 0 || dstPort >= 65536) {
            throw new IllegalArgumentException("dstPort: " + dstPort + " (expected: 1~65535)");
        }

        this.dstAddrType = dstAddrType;
        this.dstAddr = dstAddr;
        this.dstPort = dstPort;
        this.rep = rep;
    }

    public Socks5AddressType dstAddrType() {
        return dstAddrType;
    }

    public String dstAddr() {
        return dstAddr;
    }

    public int dstPort() {
        return dstPort;
    }

    public byte getRep() {
        return rep;
    }
}
