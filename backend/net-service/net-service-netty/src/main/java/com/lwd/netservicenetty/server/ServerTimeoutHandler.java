package com.lwd.netservicenetty.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 空闲超时处理：60秒无读即判定客户端掉线，关闭连接
 *
 * @author Administrator
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ServerTimeoutHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent e && e.state() == IdleState.READER_IDLE) {
            if (!ctx.channel().isWritable()) {
                return; // 背压中 setAutoRead(false) 导致的假空闲，跳过
            }
            log.warn("通道 [{}] 60秒无读，判定掉线，关闭连接", ctx.channel().id());
            ctx.close();
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }
}
