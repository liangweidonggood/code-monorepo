package com.lwd.netservicenetty.server;

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

        // 1. 当网络堵塞，触发高水位，通道变成【不可写】
        if (!ch.isWritable()) {
            log.warn("🚨 警告：通道 [{}] 积压数据超过高水位线！开始实施背压保护...", ch.id());

            // 【核武器操作】：关闭当前通道的自动读取（AutoRead）
            // 意思是：Netty 别再去读这个客户端发过来的新数据了，把数据憋在操作系统的 TCP 缓冲区里。
            // 客户端发现它的发送窗口满了，也会被迫卡住停下来，从而保护了我们服务器的内存！
            ch.config().setAutoRead(false);
        }
        // 2. 当网卡慢慢把积压数据消化掉，低于 32MB（低水位）时，通道重新变成【可写】
        else {
            log.info("🎉 喜报：通道 [{}] 积压释放，已降至低水位线以下。恢复正常通信。", ch.id());

            // 警报解除：重新开启自动读取，允许继续接收该客户端的数据
            ch.config().setAutoRead(true);
        }

        // 继续向下传递事件
        super.channelWritabilityChanged(ctx);
    }
}
