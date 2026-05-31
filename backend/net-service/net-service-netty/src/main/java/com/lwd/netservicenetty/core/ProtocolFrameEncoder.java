package com.lwd.netservicenetty.core;

import com.lwd.netservicenetty.core.internal.codec.BccUtils;
import com.lwd.netservicenetty.core.internal.codec.MessageCodecRegistry;
import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.ProtocolConstants;
import com.lwd.netservicenetty.protocol.TcpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 协议帧编码器 — TcpPacket → ByteBuf 帧
 *
 * <p>帧格式: [FE(1B)] [len(1B)] [msgType(1B)] [subMsgType(1B)] [body(N B)] [BCC(1B)]
 *
 * @author Administrator
 */
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class ProtocolFrameEncoder extends MessageToByteEncoder<TcpPacket> {

    private static final java.util.Map<String, Integer> MSG_TYPE_MAP = java.util.Map.ofEntries(
            java.util.Map.entry("CommonResponse", ProtocolConstants.MSG_COMMON_RESPONSE),
            java.util.Map.entry("RegistrationRequest", ProtocolConstants.MSG_REGISTRATION_REQUEST),
            java.util.Map.entry("AuthRequest", ProtocolConstants.MSG_AUTH_REQUEST),
            java.util.Map.entry("Heartbeat", ProtocolConstants.MSG_HEARTBEAT),
            java.util.Map.entry("LocationReport", ProtocolConstants.MSG_LOCATION_REPORT),
            java.util.Map.entry("AlarmReport", ProtocolConstants.MSG_ALARM_REPORT),
            java.util.Map.entry("DataTransmission", ProtocolConstants.MSG_DATA_TRANSMISSION),
            java.util.Map.entry("ImmediateReplayCmd", ProtocolConstants.MSG_IMMEDIATE_REPLAY),
            java.util.Map.entry("RegistrationResponse", ProtocolConstants.MSG_REGISTRATION_RESPONSE),
            java.util.Map.entry("RemoteConfigCmd", ProtocolConstants.MSG_REMOTE_CONFIG)
    );

    private final MessageCodecRegistry registry;

    @Override
    protected void encode(ChannelHandlerContext ctx, TcpPacket packet, ByteBuf out) {
        MessageCodec<TcpPacket> codec = registry.lookup(msgTypeOf(packet));
        int msgType = codec.msgType();
        byte subMsgType = ProtocolConstants.SUB_TYPE_PLAIN;

        ByteBuf bodyBuf = Unpooled.buffer();
        try {
            codec.encode(packet, bodyBuf);
            int bodyLen = bodyBuf.readableBytes();

            // 计算 BCC（必须在 write 之前，否则 readerIndex 会被消耗）
            byte bcc = BccUtils.computeFrame((byte) bodyLen, (byte) msgType, subMsgType, bodyBuf);

            out.writeByte(ProtocolConstants.MAGIC_NUMBER);
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
        Integer type = MSG_TYPE_MAP.get(packet.getClass().getSimpleName());
        if (type == null) {
            throw new IllegalArgumentException(
                    "未知消息类型: " + packet.getClass().getSimpleName());
        }
        return type;
    }
}
