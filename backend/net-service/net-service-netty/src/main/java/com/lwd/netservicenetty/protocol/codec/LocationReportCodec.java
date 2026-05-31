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
    public void encode(LocationReport pkt, ByteBuf out) {
        BcdUtils.writeBcd(out, pkt.terminalId());
        out.writeInt(BcdUtils.encodeLatLon(pkt.latitude()));
        out.writeInt(BcdUtils.encodeLatLon(pkt.longitude()));
        out.writeShort(pkt.speed());
        out.writeShort(pkt.direction());
        out.writeShort(pkt.altitude());
        out.writeInt(pkt.mileage());
        out.writeByte(pkt.fuelPercent());
        out.writeByte(pkt.engineTemp());
        out.writeShort(pkt.batteryVoltage());
        out.writeByte(pkt.signalStrength());
        out.writeByte(pkt.satellites());
        out.writeInt((int) pkt.alarmFlags());
        BcdUtils.writeGpsTime(out, pkt.gpsTime());
        out.writeShort(pkt.seqNo());
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
