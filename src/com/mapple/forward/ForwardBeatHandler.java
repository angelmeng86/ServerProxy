package com.mapple.forward;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ForwardBeatHandler extends ChannelDuplexHandler {
    
    public static final ForwardBeatHandler INSTANCE = new ForwardBeatHandler();
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                System.out.println("关闭连接-----");
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                System.out.println("发送心跳包-----");
                ctx.writeAndFlush(new ForwardBeat());
            }
        }
    }
}
