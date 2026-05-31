package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.ProtocolConstants;
import com.lwd.netservicenetty.protocol.DataTransmission;
import io.netty.buffer.ByteBuf;
import org.springframework.stereotype.Component;

/**
 * 数据透传编解码器
 *
 * @author Administrator
 */
@Component
public class DataTransmissionCodec implements MessageCodec<DataTransmission> {

    @Override
    public int msgType() {
        return ProtocolConstants.MSG_DATA_TRANSMISSION;
    }

    @Override
    public void encode(DataTransmission pkt, ByteBuf out) {
        BcdUtils.writeBcd(out, pkt.terminalId());
        out.writeShort(pkt.dataType());
        out.writeShort(pkt.payload().length);
        out.writeBytes(pkt.payload());
    }

    @Override
    public DataTransmission decode(ByteBuf in) {
        String terminalId = BcdUtils.readBcd(in, 7);
        int dataType = in.readUnsignedShort();
        int payloadLen = in.readUnsignedShort();
        byte[] payload = new byte[payloadLen];
        in.readBytes(payload);
        return new DataTransmission(terminalId, dataType, payload);
    }
}
