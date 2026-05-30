package com.lwd.netservicenetty.core.internal.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Administrator
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ProtocolGuardHandler extends ChannelInboundHandlerAdapter {

    private static final short MAGIC_NUMBER = 0xFE;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        if (in.readableBytes() < 1) {
            ctx.fireChannelRead(in);
            return;
        }
        // 窥探第一位
        short magic = in.getUnsignedByte(in.readerIndex());
        if (magic != MAGIC_NUMBER) {
            // 1. 打印警报
            log.error("【安全警报】非法魔数: {}，指针对齐已乱，强行掐断连接！", Integer.toHexString(magic));
            // 2. 把水管里的脏数据全部读光（直接把读指针移动到最后）
            in.skipBytes(in.readableBytes());
            in.release();
            // 3. 异步断开连接
            ctx.close();
            return;
        }
        // 对了就放行，交棒给后面的长度拆包器
        ctx.fireChannelRead(in);
    }
}
