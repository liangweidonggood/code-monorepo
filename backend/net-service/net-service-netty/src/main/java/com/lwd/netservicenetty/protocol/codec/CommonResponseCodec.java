package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.ProtocolConstants;
import com.lwd.netservicenetty.protocol.CommonResponse;
import io.netty.buffer.ByteBuf;
import org.springframework.stereotype.Component;

/**
 * 通用应答编解码器
 *
 * @author Administrator
 */
@Component
public class CommonResponseCodec implements MessageCodec<CommonResponse> {

    @Override
    public int msgType() {
        // 通用应答消息类型: 0x00 (Client→Server) / 0x80 (Server→Client)，统一用 0x00 标识
        return ProtocolConstants.MSG_COMMON_RESPONSE;
    }

    @Override
    public void encode(CommonResponse packet, ByteBuf out) {
        out.writeShort(packet.serialNo());
        out.writeByte(packet.respMsgType());
        out.writeByte(packet.result());
    }

    @Override
    public CommonResponse decode(ByteBuf in) {
        int serialNo = in.readUnsignedShort();
        short respMsgType = in.readUnsignedByte();
        int result = in.readUnsignedByte();
        return new CommonResponse(serialNo, respMsgType, result);
    }
}
