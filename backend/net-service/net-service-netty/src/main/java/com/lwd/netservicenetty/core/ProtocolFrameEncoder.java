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

    private final MessageCodecRegistry registry;

    public ProtocolFrameEncoder(MessageCodecRegistry registry) {
        this.registry = registry;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void encode(ChannelHandlerContext ctx, TcpPacket packet, ByteBuf out) {
        MessageCodec<TcpPacket> codec = (MessageCodec<TcpPacket>) registry.lookup(msgTypeOf(packet));
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
        if (packet instanceof com.lwd.netservicenetty.protocol.CommonResponse) {
            return ProtocolConstants.MSG_COMMON_RESPONSE;
        }
        if (packet instanceof com.lwd.netservicenetty.protocol.RegistrationRequest) {
            return ProtocolConstants.MSG_REGISTRATION_REQUEST;
        }
        if (packet instanceof com.lwd.netservicenetty.protocol.AuthRequest) {
            return ProtocolConstants.MSG_AUTH_REQUEST;
        }
        if (packet instanceof com.lwd.netservicenetty.protocol.Heartbeat) {
            return ProtocolConstants.MSG_HEARTBEAT;
        }
        if (packet instanceof com.lwd.netservicenetty.protocol.LocationReport) {
            return ProtocolConstants.MSG_LOCATION_REPORT;
        }
        if (packet instanceof com.lwd.netservicenetty.protocol.AlarmReport) {
            return ProtocolConstants.MSG_ALARM_REPORT;
        }
        if (packet instanceof com.lwd.netservicenetty.protocol.DataTransmission) {
            return ProtocolConstants.MSG_DATA_TRANSMISSION;
        }
        if (packet instanceof com.lwd.netservicenetty.protocol.ImmediateReplayCmd) {
            return ProtocolConstants.MSG_IMMEDIATE_REPLAY;
        }
        if (packet instanceof com.lwd.netservicenetty.protocol.RegistrationResponse) {
            return ProtocolConstants.MSG_REGISTRATION_RESPONSE;
        }
        if (packet instanceof com.lwd.netservicenetty.protocol.RemoteConfigCmd) {
            return ProtocolConstants.MSG_REMOTE_CONFIG;
        }
        throw new IllegalArgumentException("未知消息类型: " + packet.getClass().getSimpleName());
    }
}
