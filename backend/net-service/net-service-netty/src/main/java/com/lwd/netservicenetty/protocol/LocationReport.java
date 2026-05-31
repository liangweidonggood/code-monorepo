package com.lwd.netservicenetty.protocol;


import java.time.LocalDateTime;

/**
 * 位置上报 — 核心业务消息，包含 GPS 坐标、车速、油量等 15 个字段。
 *
 * @author Administrator
 */
public record LocationReport(
        String terminalId,
        double latitude,
        double longitude,
        int speed,
        int direction,
        int altitude,
        int mileage,
        int fuelPercent,
        int engineTemp,
        int batteryVoltage,
        int signalStrength,
        int satellites,
        long alarmFlags,
        LocalDateTime gpsTime,
        int seqNo
) implements TcpPacket {
}
