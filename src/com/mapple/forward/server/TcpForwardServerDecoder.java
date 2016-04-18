package com.mapple.forward.server;

import com.mapple.forward.ForwardBeat;
import com.mapple.forward.ForwardCmd;
import com.mapple.forward.ForwardConnectAck;
import com.mapple.forward.ForwardData;
import com.mapple.forward.ForwardDisconnect;
import com.mapple.forward.ForwardLogin;
import com.mapple.forward.ForwardVersion;
import com.mapple.forward.server.TcpForwardServerDecoder.State;
import com.mapple.socksproxy.SocksServerUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

public class TcpForwardServerDecoder extends ReplayingDecoder<State> {
    
	private byte[] uid = new byte[6];
    private final Socks5AddressDecoder addressDecoder;
    
    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(TcpForwardServerDecoder.class);
    
	enum State {
        START,
        LOGIN,
        BEAT,
        CONNECTACK,
        DATA,
        DISCONNECT
    }

	public TcpForwardServerDecoder() {
        super(State.START);
        this.addressDecoder = Socks5AddressDecoder.DEFAULT;
    }
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
		List<Object> out) throws Exception {
	    System.out.println("123服务端接收数据----- ");
		switch (state()) {
		case START: {
		    System.out.println("服务端接收数据-----");
			final byte version = in.readByte();
			if (version != ForwardVersion.FORWARD1.byteValue()) {
				throw new DecoderException("unsupported version: "
						+ version + " (expected: "
						+ ForwardVersion.FORWARD1.byteValue() + ')');
			}

			ForwardCmd cmd = ForwardCmd.valueOf(in.readByte());
			State state = State.START;
			switch (cmd) {
			case LOGIN:
				state = State.LOGIN;
				break;
			case BEAT:
				state = State.BEAT;
				break;
			case CONNECTACK:
				state = State.CONNECTACK;
				break;
			case DATA:
				state = State.DATA;
				break;
			default:
				throw new DecoderException("unsupported cmd: (expected: " + cmd.byteValue() + ')');
			}
			
			final byte rsv = in.readByte();
			if(rsv != 0x00) {
				throw new DecoderException("unsupported rsv: (expected: " + rsv + ')');
			}
			
			checkpoint(state);
		}
		case LOGIN: {
		    System.out.println("服务端接收登录-----");
			final int len = in.readUnsignedByte();
			final String name = in.toString(in.readerIndex(), len, CharsetUtil.UTF_8);
			in.skipBytes(len);
			
			out.add(new ForwardLogin(name));
			checkpoint(State.START);
		}
			break;
		case BEAT: {
		    System.out.println("服务端接收到心跳包-----");
//			ctx.writeAndFlush(new ForwardBeat());
			checkpoint(State.START);
		}
			break;
		case CONNECTACK: {
		    in.getBytes(in.readerIndex(), uid);
		    final String addr = NetUtil.intToIpAddress(in.readInt());
		    final int port = in.readUnsignedShort();
			
			final byte rep = in.readByte();
			
			final Socks5AddressType dstAddrType = Socks5AddressType.valueOf(in.readByte());
			final String dstAddr = addressDecoder.decodeAddress(dstAddrType, in);
            final int dstPort = in.readUnsignedShort();
            out.add(new ForwardConnectAck(uid, addr, port, rep, dstAddrType, dstAddr, dstPort));
            checkpoint(State.START);
		}
			break;
		case DATA: {
		    in.getBytes(in.readerIndex(), uid);
            final String addr = NetUtil.intToIpAddress(in.readInt());
            final int port = in.readUnsignedShort();
            
            final int len = in.readUnsignedShort();
            if (actualReadableBytes() < len) {
                break;
            }
            
            out.add(new ForwardData(uid, addr, port, in.readSlice(len).retain()));
            checkpoint(State.START);
		}
			break;
		case DISCONNECT: {
		    in.getBytes(in.readerIndex(), uid);
            final String addr = NetUtil.intToIpAddress(in.readInt());
            final int port = in.readUnsignedShort();
            
            out.add(new ForwardDisconnect(uid, addr, port));
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
