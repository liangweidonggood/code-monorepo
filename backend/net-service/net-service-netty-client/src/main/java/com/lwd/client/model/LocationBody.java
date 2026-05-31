package com.lwd.client.model;

import java.time.LocalDateTime;

/**
 * 位置上报消息体 — 包含 GPS 坐标、车速、方向、油量、卫星数等。
 *
 * @author Administrator
 */
public record LocationBody(
        String terminalId,       // 终端号（14 位数字，BCD 编码）
        double lat,              // 纬度
        double lng,              // 经度
        int speed,               // 速度 km/h
        int direction,           // 方向 0-359°
        int altitude,            // 海拔 m
        int mileage,             // 里程 km
        int fuelPercent,         // 油量百分比
        int engineTemp,          // 发动机温度 ℃
        int batteryVoltage,      // 电瓶电压 mV
        int signalStrength,      // 信号强度
        int satellites,          // 卫星数
        long alarmFlags,         // 告警标志位
        LocalDateTime gpsTime,   // GPS 时间
        int seqNo                // 流水号
) {
}
