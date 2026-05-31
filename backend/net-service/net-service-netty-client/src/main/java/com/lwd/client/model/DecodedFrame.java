package com.lwd.client.model;

import io.netty.buffer.ByteBuf;

/**
 * 解码后的帧信息 — 消息类型码 + body slice。
 *
 * @author Administrator
 */
public record DecodedFrame(
        int msgType,  // 消息类型码
        ByteBuf body  // 消息体 slice（不包含 BCC）
) {
}
