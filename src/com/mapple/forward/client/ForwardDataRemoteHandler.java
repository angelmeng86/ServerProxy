package com.mapple.forward.client;

import com.mapple.forward.ForwardAddrMessage;
import com.mapple.forward.ForwardData;
import com.mapple.socksproxy.SocksServerUtils;

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
        System.out.println("ForwardDataRemoteHandler1:" + ctx + " " + in.readableBytes());
        if (localChannel.isActive()) {
            while(in.readableBytes() > 0) {
                if(in.readableBytes() < 200) {
                localChannel.writeAndFlush(new ForwardData(addr, in.readSlice(in.readableBytes())));
                }
                else {
                    localChannel.writeAndFlush(new ForwardData(addr, in.readSlice(200)));
                }
            }
        }
        else {
            SocksServerUtils.closeOnFlush(ctx.channel());
        }
    }

}
