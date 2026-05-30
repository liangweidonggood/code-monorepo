package com.lwd.netservicenetty.core;

import com.lwd.netservicenetty.core.internal.codec.BccUtils;
import com.lwd.netservicenetty.core.internal.codec.MessageCodecRegistry;
import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.TcpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.springframework.stereotype.Component;

/**
 * 协议帧编码器 — TcpPacket → ByteBuf 帧
 * <p>
 * 帧格式: [FE(1B)] [len(1B)] [msgType(1B)] [subMsgType(1B)] [body(N B)] [BCC(1B)]
 *
 * @author Administrator
 */
@Component
@ChannelHandler.Sharable
public class ProtocolFrameEncoder extends MessageToByteEncoder<TcpPacket> {

    private static final byte MAGIC = (byte) 0xFE;

    private final MessageCodecRegistry registry;

    public ProtocolFrameEncoder(MessageCodecRegistry registry) {
        this.registry = registry;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void encode(ChannelHandlerContext ctx, TcpPacket packet, ByteBuf out) {
        MessageCodec<TcpPacket> codec = (MessageCodec<TcpPacket>) registry.lookup(msgTypeOf(packet));
        int msgType = codec.msgType();
        byte subMsgType = 0; // 加密子类型扩展点

        ByteBuf bodyBuf = Unpooled.buffer();
        try {
            codec.encode(packet, bodyBuf);
            int bodyLen = bodyBuf.readableBytes();

            // 计算 BCC（必须在 write 之前，否则 readerIndex 会被消耗）
            byte bcc = BccUtils.computeFrame((byte) bodyLen, (byte) msgType, subMsgType, bodyBuf);

            out.writeByte(MAGIC);
            out.writeByte(bodyLen);
            out.writeByte(msgType);
            out.writeByte(subMsgType);
            out.writeBytes(bodyBuf);
            out.writeByte(bcc);
        } finally {
            bodyBuf.release();
        }
    }

    /** 暴露给单元测试 */
    public ByteBuf encode(TcpPacket packet) {
        ByteBuf out = Unpooled.buffer();
        encode(null, packet, out);
        return out;
    }

    private int msgTypeOf(TcpPacket packet) {
        if (packet instanceof com.lwd.netservicenetty.protocol.CommonResponse) return 0x00;
        if (packet instanceof com.lwd.netservicenetty.protocol.RegistrationRequest) return 0x01;
        if (packet instanceof com.lwd.netservicenetty.protocol.AuthRequest) return 0x02;
        if (packet instanceof com.lwd.netservicenetty.protocol.Heartbeat) return 0x03;
        if (packet instanceof com.lwd.netservicenetty.protocol.LocationReport) return 0x10;
        if (packet instanceof com.lwd.netservicenetty.protocol.AlarmReport) return 0x11;
        if (packet instanceof com.lwd.netservicenetty.protocol.DataTransmission) return 0x12;
        if (packet instanceof com.lwd.netservicenetty.protocol.ImmediateReplayCmd) return 0x82;
        if (packet instanceof com.lwd.netservicenetty.protocol.RegistrationResponse) return 0x81;
        if (packet instanceof com.lwd.netservicenetty.protocol.RemoteConfigCmd) return 0x83;
        throw new IllegalArgumentException("未知消息类型: " + packet.getClass().getSimpleName());
    }
}
