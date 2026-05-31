package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.ProtocolConstants;
import com.lwd.netservicenetty.protocol.RegistrationResponse;
import io.netty.buffer.ByteBuf;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 注册应答编解码器
 *
 * @author Administrator
 */
@Component
public class RegistrationResponseCodec implements MessageCodec<RegistrationResponse> {

    @Override
    public int msgType() {
        return ProtocolConstants.MSG_REGISTRATION_RESPONSE;
    }

    @Override
    public void encode(RegistrationResponse pkt, ByteBuf out) {
        out.writeShort(pkt.seqNo());
        out.writeByte(pkt.result());
        BcdUtils.writeBcd(out, pkt.authCode());
        byte[] msgBytes = pkt.message().getBytes(StandardCharsets.US_ASCII);
        out.writeByte(msgBytes.length);
        out.writeBytes(msgBytes);
    }

    @Override
    public RegistrationResponse decode(ByteBuf in) {
        int seqNo = in.readUnsignedShort();
        int result = in.readUnsignedByte();
        String authCode = BcdUtils.readBcd(in, 4);
        int msgLen = in.readUnsignedByte();
        byte[] msgBytes = new byte[msgLen];
        in.readBytes(msgBytes);
        String message = new String(msgBytes, StandardCharsets.US_ASCII);
        return new RegistrationResponse(seqNo, result, authCode, message);
    }
}
