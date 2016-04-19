package com.mapple.forward;

import java.net.IDN;

import io.netty.channel.Channel;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.util.NetUtil;

public class ForwardConnect extends ForwardAddrMessage {
    
    private final Socks5AddressType dstAddrType;
    private final String dstAddr;
    private final int dstPort;
    
    private Channel srcChannel;

    @Override
    public ForwardCmd cmd() {
        return ForwardCmd.CONNECT;
    }

    public ForwardConnect(String srcAddr, int srcPort, Socks5AddressType dstAddrType, String dstAddr, int dstPort) {
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

    public Channel getSrcChannel() {
        return srcChannel;
    }

    public void setSrcChannel(Channel srcChannel) {
        this.srcChannel = srcChannel;
    }

}
