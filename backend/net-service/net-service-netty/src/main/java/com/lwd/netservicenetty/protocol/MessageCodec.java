package com.lwd.netservicenetty.protocol;

import io.netty.buffer.ByteBuf;

/**
 * 消息编解码器 SPI 接口 (DIP)
 * <p>
 * 每种消息类型一个 Codec 实现，新增消息无需改动解码器框架。
 *
 * @param <T> 消息 POJO 类型
 * @author Administrator
 */
public interface MessageCodec<T extends TcpPacket> {

    /** 消息类型标识 */
    int msgType();

    /** 将 POJO 编码写入 ByteBuf */
    void encode(T packet, ByteBuf out);

    /** 从 ByteBuf 解码为 POJO */
    T decode(ByteBuf in);
}
