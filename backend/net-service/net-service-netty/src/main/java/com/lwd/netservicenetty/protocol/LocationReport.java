package com.lwd.netservicenetty.protocol;


import java.time.LocalDateTime;

/**
 * 位置上报 — 核心业务消息，15 个字段
 * <p>
 * Body: [终端号(14B BCD)] + [纬度(4B)] + [经度(4B)] + [速度(2B)] + [方向(2B)]
 * + [海拔(2B)] + [里程(4B)] + [油量(1B)] + [发动机温度(1B)] + [电池电压(2B)]
 * + [信号强度(1B)] + [卫星数(1B)] + [报警位(4B)] + [GPS时间(6B BCD)] + [流水号(2B)] = 50 字节
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
