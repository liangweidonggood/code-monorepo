package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.ProtocolConstants;
import com.lwd.netservicenetty.protocol.AuthRequest;
import io.netty.buffer.ByteBuf;
import org.springframework.stereotype.Component;

/**
 * 鉴权请求编解码器
 *
 * @author Administrator
 */
@Component
public class AuthRequestCodec implements MessageCodec<AuthRequest> {

    @Override
    public int msgType() {
        return ProtocolConstants.MSG_AUTH_REQUEST;
    }

    @Override
    public void encode(AuthRequest packet, ByteBuf out) {
        BcdUtils.writeBcd(out, packet.terminalId());
        BcdUtils.writeBcd(out, packet.authCode());
        out.writeShort(packet.seqNo());
    }

    @Override
    public AuthRequest decode(ByteBuf in) {
        String terminalId = BcdUtils.readBcd(in, 7);
        String authCode = BcdUtils.readBcd(in, 4);  // 8 chars → 4 bytes BCD
        int seqNo = in.readUnsignedShort();
        return new AuthRequest(terminalId, authCode, seqNo);
    }
}
