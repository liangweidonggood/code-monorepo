package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.MessageCodec;
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
        return 0x12;
    }

    @Override
    public void encode(DataTransmission p, ByteBuf out) {
        BcdUtils.writeBcd(out, p.terminalId());
        out.writeShort(p.dataType());
        out.writeShort(p.payload().length);
        out.writeBytes(p.payload());
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
