package com.lwd.netservicenetty.core.internal.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 背压机制
 *
 * @author Administrator
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class SafeTrafficHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel ch = ctx.channel();

        if (ch.isWritable()) {
            log.info("🎉 喜报：通道 [{}] 积压释放，已降至低水位线以下。恢复正常通信。", ch.id());
            ch.config().setAutoRead(true);
        } else {
            log.warn("🚨 警告：通道 [{}] 积压数据超过高水位线！开始实施背压保护...", ch.id());
            ch.config().setAutoRead(false);
        }

        // 继续向下传递事件
        super.channelWritabilityChanged(ctx);
    }
}
