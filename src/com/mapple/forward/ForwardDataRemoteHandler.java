package com.mapple.forward;

import com.mapple.forward.ForwardAddrMessage;
import com.mapple.forward.ForwardData;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class ForwardDataRemoteHandler extends ByteToMessageDecoder {

    private final Channel localChannel;
    private final ForwardAddrMessage addr;

    public ForwardDataRemoteHandler(Channel localChannel, ForwardAddrMessage addr) {
        this.localChannel = localChannel;
        this.addr = addr;
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (localChannel.isActive()) {
            while(in.readableBytes() > 0) {
                System.out.print("client readlen:" + in.readableBytes());
                if(in.readableBytes() < 8096) {
                    localChannel.writeAndFlush(new ForwardData(addr, in.readSlice(in.readableBytes()).retain()));
                }
                else {
                    localChannel.writeAndFlush(new ForwardData(addr, in.readSlice(8096).retain()));
                }
            }
//            localChannel.writeAndFlush(new ForwardData(addr, in.readSlice(in.readableBytes()).retain()));
        }
        else {
            ForwardUtils.closeOnFlush(ctx.channel());
        }
    }

}
