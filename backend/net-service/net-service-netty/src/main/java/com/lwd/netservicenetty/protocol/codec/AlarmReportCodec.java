package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.ProtocolConstants;
import com.lwd.netservicenetty.protocol.AlarmReport;
import io.netty.buffer.ByteBuf;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 告警上报编解码器
 *
 * @author Administrator
 */
@Component
public class AlarmReportCodec implements MessageCodec<AlarmReport> {

    @Override
    public int msgType() {
        return ProtocolConstants.MSG_ALARM_REPORT;
    }

    @Override
    public void encode(AlarmReport p, ByteBuf out) {
        BcdUtils.writeBcd(out, p.terminalId());
        out.writeShort(p.alarmType());
        out.writeByte(p.alarmLevel());
        out.writeInt(BcdUtils.encodeLatLon(p.latitude()));
        out.writeInt(BcdUtils.encodeLatLon(p.longitude()));
        out.writeShort(p.speed());
        BcdUtils.writeGpsTime(out, p.alarmTime());
        byte[] descBytes = p.description().getBytes(StandardCharsets.US_ASCII);
        out.writeByte(descBytes.length);
        out.writeBytes(descBytes);
    }

    @Override
    public AlarmReport decode(ByteBuf in) {
        String terminalId = BcdUtils.readBcd(in, 7);
        int alarmType = in.readUnsignedShort();
        int alarmLevel = in.readUnsignedByte();
        double lat = BcdUtils.decodeLatLon(in.readInt());
        double lng = BcdUtils.decodeLatLon(in.readInt());
        int speed = in.readUnsignedShort();
        java.time.LocalDateTime alarmTime = BcdUtils.readGpsTime(in);
        int descLen = in.readUnsignedByte();
        byte[] descBytes = new byte[descLen];
        in.readBytes(descBytes);
        String description = new String(descBytes, StandardCharsets.US_ASCII);
        return new AlarmReport(terminalId, alarmType, alarmLevel, lat, lng, speed, alarmTime, description);
    }
}
