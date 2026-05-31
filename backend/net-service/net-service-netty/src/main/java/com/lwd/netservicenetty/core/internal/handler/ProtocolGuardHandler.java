package com.lwd.netservicenetty.core.internal.handler;

import com.lwd.netservicenetty.protocol.ProtocolConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 协议魔数校验 — 检查帧首字节是否 0xFE，非法则丢弃+告警+掐断连接。
 *
 * <p>放在 {@code LengthFieldBasedFrameDecoder} 之前，拦截错误数据防止拆包器解析异常。
 *
 * @author Administrator
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ProtocolGuardHandler extends ChannelInboundHandlerAdapter {

    /** 最小帧长度：至少包含魔数字节 */
    private static final int MIN_FRAME_SIZE = 1;

    /**
     * 校验帧头魔数，合法帧放行，非法帧告警并掐断。
     *
     * @param ctx 通道上下文
     * @param msg 原始字节数据
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        if (in.readableBytes() < MIN_FRAME_SIZE) {
            ctx.fireChannelRead(in);
            return;
        }
        // getByte 返回有符号 byte，与 MAGIC_NUMBER 类型一致，避免无符号比较 bug
        byte magic = in.getByte(in.readerIndex());
        if (magic != ProtocolConstants.MAGIC_NUMBER) {
            log.error("【安全警报】非法魔数: {}，指针对齐已乱，强行掐断连接！",
                    Integer.toHexString(magic & 0xFF));
            in.skipBytes(in.readableBytes());
            in.release();
            ctx.close();
            return;
        }
        ctx.fireChannelRead(in);
    }
}
