package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.RegistrationRequest;
import io.netty.buffer.ByteBuf;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 注册请求编解码器 — 包含变长字段（车牌号）
 *
 * @author Administrator
 */
@Component
public class RegistrationRequestCodec implements MessageCodec<RegistrationRequest> {

    @Override
    public int msgType() {
        return 0x01;
    }

    @Override
    public void encode(RegistrationRequest p, ByteBuf out) {
        BcdUtils.writeBcd(out, p.terminalId());
        out.writeShort(p.manufacturerId());
        BcdUtils.writeBcd(out, p.terminalModel());
        out.writeByte(p.hardwareVersion());
        out.writeByte(p.softwareVersion());
        out.writeShort(p.provinceId());
        out.writeShort(p.cityId());
        byte[] plateBytes = p.licensePlate().getBytes(StandardCharsets.US_ASCII);
        out.writeByte(plateBytes.length);
        out.writeBytes(plateBytes);
        out.writeByte(p.color());
    }

    @Override
    public RegistrationRequest decode(ByteBuf in) {
        String terminalId = BcdUtils.readBcd(in, 7);
        int manufacturerId = in.readUnsignedShort();
        String terminalModel = BcdUtils.readBcd(in, 2);
        int hardwareVersion = in.readUnsignedByte();
        int softwareVersion = in.readUnsignedByte();
        int provinceId = in.readUnsignedShort();
        int cityId = in.readUnsignedShort();
        int plateLen = in.readUnsignedByte();
        byte[] plateBytes = new byte[plateLen];
        in.readBytes(plateBytes);
        String licensePlate = new String(plateBytes, StandardCharsets.US_ASCII);
        int color = in.readUnsignedByte();
        return new RegistrationRequest(terminalId, manufacturerId, terminalModel,
                hardwareVersion, softwareVersion, provinceId, cityId, licensePlate, color);
    }
}
