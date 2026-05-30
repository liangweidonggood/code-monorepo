package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.ProtocolConstants;
import com.lwd.netservicenetty.protocol.LocationReport;
import io.netty.buffer.ByteBuf;
import org.springframework.stereotype.Component;

/**
 * 位置上报编解码器
 *
 * @author Administrator
 */
@Component
public class LocationReportCodec implements MessageCodec<LocationReport> {

    @Override
    public int msgType() {
        return ProtocolConstants.MSG_LOCATION_REPORT;
    }

    @Override
    public void encode(LocationReport p, ByteBuf out) {
        BcdUtils.writeBcd(out, p.terminalId());
        out.writeInt(BcdUtils.encodeLatLon(p.latitude()));
        out.writeInt(BcdUtils.encodeLatLon(p.longitude()));
        out.writeShort(p.speed());
        out.writeShort(p.direction());
        out.writeShort(p.altitude());
        out.writeInt(p.mileage());
        out.writeByte(p.fuelPercent());
        out.writeByte(p.engineTemp());
        out.writeShort(p.batteryVoltage());
        out.writeByte(p.signalStrength());
        out.writeByte(p.satellites());
        out.writeInt((int) p.alarmFlags());
        BcdUtils.writeGpsTime(out, p.gpsTime());
        out.writeShort(p.seqNo());
    }

    @Override
    public LocationReport decode(ByteBuf in) {
        String terminalId = BcdUtils.readBcd(in, 7);
        double lat = BcdUtils.decodeLatLon(in.readInt());
        double lng = BcdUtils.decodeLatLon(in.readInt());
        int speed = in.readUnsignedShort();
        int direction = in.readUnsignedShort();
        int altitude = in.readUnsignedShort();
        int mileage = in.readInt();
        int fuelPercent = in.readUnsignedByte();
        int engineTemp = in.readUnsignedByte();
        int batteryVoltage = in.readUnsignedShort();
        int signalStrength = in.readUnsignedByte();
        int satellites = in.readUnsignedByte();
        long alarmFlags = in.readUnsignedInt();
        java.time.LocalDateTime gpsTime = BcdUtils.readGpsTime(in);
        int seqNo = in.readUnsignedShort();
        return new LocationReport(terminalId, lat, lng, speed, direction, altitude,
                mileage, fuelPercent, engineTemp, batteryVoltage, signalStrength,
                satellites, alarmFlags, gpsTime, seqNo);
    }
}
