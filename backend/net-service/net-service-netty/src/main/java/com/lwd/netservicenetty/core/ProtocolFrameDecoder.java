package com.lwd.netservicenetty.core;

import com.lwd.netservicenetty.core.internal.codec.BccUtils;
import com.lwd.netservicenetty.core.internal.codec.MessageCodecRegistry;
import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.TcpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 协议帧解码器 — ByteBuf 帧 → TcpPacket
 * <p>
 * 处理完整帧（LengthFieldBasedFrameDecoder 已拆好），提取 body 后委托 Codec 解码。
 *
 * @author Administrator
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ProtocolFrameDecoder extends MessageToMessageDecoder<ByteBuf> {

    private final MessageCodecRegistry registry;

    public ProtocolFrameDecoder(MessageCodecRegistry registry) {
        this.registry = registry;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void decode(ChannelHandlerContext ctx, ByteBuf frame, java.util.List<Object> out) {
        int headerStart = frame.readerIndex();

        frame.readByte(); // FE
        int bodyLen = frame.readUnsignedByte();
        int msgType = frame.readUnsignedByte();
        int subMsgType = frame.readUnsignedByte();

        // BCC 校验：从 length 字节开始（跳过 FE），到 body 结束
        int bccDataLen = 3 + bodyLen;
        byte receivedBcc = frame.getByte(headerStart + 1 + bccDataLen);
        byte computed = BccUtils.computeFull(frame, headerStart + 1, bccDataLen);
        if (computed != receivedBcc) {
            log.error("【BCC校验失败】帧 BCC:0x{}, 计算:0x{}",
                    Integer.toHexString(receivedBcc & 0xFF).toUpperCase(),
                    Integer.toHexString(computed & 0xFF).toUpperCase());
            frame.skipBytes(bccDataLen + 1);
            return;
        }

        ByteBuf body = frame.readSlice(bodyLen);
        frame.readByte(); // skip BCC

        MessageCodec<TcpPacket> codec = (MessageCodec<TcpPacket>) registry.lookup(msgType);
        TcpPacket packet = codec.decode(body);
        out.add(packet);
    }

    /** 暴露给单元测试 */
    public Object decode(ByteBuf frame) {
        java.util.List<Object> out = new java.util.ArrayList<>();
        decode(null, frame, out);
        return out.isEmpty() ? null : out.get(0);
    }
}
