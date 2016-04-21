package com.mapple.forward.server;

import com.mapple.forward.ForwardBeat;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ForwardBeatHandler extends ChannelDuplexHandler {
    
    private int trytimes = 5;
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(new ForwardBeat());
                if(!ForwardQueryAddress.hasAddress(ctx.channel())) {
                    if(--trytimes > 0) {
                        ForwardQueryAddress.queryAddress(ctx.channel());
                    }
                    else {
                        ctx.close();
                    }
                }
                
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}
