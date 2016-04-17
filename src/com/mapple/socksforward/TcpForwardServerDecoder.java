package com.mapple.socksforward;

import java.nio.charset.Charset;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5Message;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;

import com.mapple.socksforward.TcpForwardServerDecoder.State;

public class TcpForwardServerDecoder extends ReplayingDecoder<State> {
	
	private final ByteBuf buf = Unpooled.buffer(4096);
	private String addr;
    private int port;
	
	enum State {
        START,
        LOGIN,
        BEAT,
        CONNECTACK,
        DATA,
        FAILURE
    }

	public TcpForwardServerDecoder() {
        super(State.START);
    }
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		try {
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
				int len = in.readUnsignedByte();
				buf.clear();
				in.readBytes(buf, len);
				
				out.add(new ForwardLoginRequest(buf.toString(CharsetUtil.UTF_8)));
				checkpoint(State.START);
			}
				break;
			case BEAT: {
				out.add(new ForwardBeat());
				checkpoint(State.START);
			}
				break;
			case CONNECTACK: {
				port = in.readUnsignedShort();
                addr = NetUtil.intToIpAddress(in.readInt());
			}
				break;
			case DATA: {

			}
				break;
			case FAILURE: {
				in.skipBytes(actualReadableBytes());
				break;
			}
			}
		} catch (Exception e) {
			fail(out, e);
		}
	}
	
	private void fail(List<Object> out, Throwable cause) {
        if (!(cause instanceof DecoderException)) {
            cause = new DecoderException(cause);
        }

        checkpoint(State.FAILURE);

        Socks5Message m = new DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH);
        m.setDecoderResult(DecoderResult.failure(cause));
        out.add(m);
    }
}
