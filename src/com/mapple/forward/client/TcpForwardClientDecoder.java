package com.mapple.forward.client;

import com.mapple.forward.ForwardBeat;
import com.mapple.forward.ForwardCmd;
import com.mapple.forward.ForwardConnect;
import com.mapple.forward.ForwardData;
import com.mapple.forward.ForwardDisconnect;
import com.mapple.forward.ForwardLogin;
import com.mapple.forward.ForwardVersion;
import com.mapple.forward.client.TcpForwardClientDecoder.State;
import com.mapple.socksproxy.SocksServerUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.util.NetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

public class TcpForwardClientDecoder extends ReplayingDecoder<State> {
    
    private final Socks5AddressDecoder addressDecoder;
    
    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(TcpForwardClientDecoder.class);
    
    enum State {
        START,
        LOGINACK,
        CONNECT,
        DATA,
        DISCONNECT
    }

    public TcpForwardClientDecoder() {
        super(State.START);
        this.addressDecoder = Socks5AddressDecoder.DEFAULT;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ctx.writeAndFlush(new ForwardLogin("LWZ"));
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in,
        List<Object> out) throws Exception {
        switch (state()) {
        case START: {
            final byte version = in.readByte();
            if (version != ForwardVersion.FORWARD1.byteValue()) {
                throw new DecoderException("unsupported version: "
                        + version + " (expected: "
                        + ForwardVersion.FORWARD1.byteValue() + ')');
            }

            ForwardCmd cmd = ForwardCmd.valueOf(in.readByte());
            State state = State.START;
            switch (cmd) {
            case LOGINACK:
                state = State.LOGINACK;
                break;
            case BEAT: {
                ctx.writeAndFlush(new ForwardBeat());
            }
                break;
            case CONNECT:
                state = State.CONNECT;
                break;
            case DATA:
                state = State.DATA;
                break;
            case DISCONNECT:
                state = State.DISCONNECT;
                break;
            default:
                throw new DecoderException("unsupported cmd: (expected: " + String.format("0x%02x", cmd.byteValue()) + ')');
            }

            final byte rsv = in.readByte();
            if(rsv != 0x00) {
                throw new DecoderException("unsupported rsv: (expected: " + rsv + ')');
            }
            System.out.println("客户端CMD " + String.format("0x%02x", cmd.byteValue()));
            checkpoint(state);
        }
        	break;
        case LOGINACK: {
            final byte rep = in.readByte();
            if(rep != 0x00) {
                throw new DecoderException("login failed rep: " + rep);
            }
            checkpoint(State.START);
        }
            break;
        case CONNECT: {
            final String addr = NetUtil.intToIpAddress(in.readInt());
            final int port = in.readUnsignedShort();
            
            final Socks5AddressType dstAddrType = Socks5AddressType.valueOf(in.readByte());
            final String dstAddr = addressDecoder.decodeAddress(dstAddrType, in);
            final int dstPort = in.readUnsignedShort();
            out.add(new ForwardConnect(addr, port, dstAddrType, dstAddr, dstPort));
            checkpoint(State.START);
        }
            break;
        case DATA: {
            final String addr = NetUtil.intToIpAddress(in.readInt());
            final int port = in.readUnsignedShort();
            
            final int len = in.readUnsignedByte();
            System.out.println("客户端DATA len:" + len);
            if(len > 0) {
                if (actualReadableBytes() < len) {
                    break;
                }
                
                out.add(new ForwardData(addr, port, in.readSlice(len)));
            }
            checkpoint(State.START);
        }
            break;
        case DISCONNECT: {
            final String addr = NetUtil.intToIpAddress(in.readInt());
            final int port = in.readUnsignedShort();
            
            out.add(new ForwardDisconnect(addr, port));
            checkpoint(State.START);
        }
            break;
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        SocksServerUtils.closeOnFlush(ctx.channel());
    }
}
