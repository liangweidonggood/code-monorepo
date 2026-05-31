package com.lwd.client.model;

/**
 * GPS 采样点数据对象。
 *
 * @author Administrator
 */
public record GpsPoint(
        double latitude,     // 纬度
        double longitude,    // 经度
        int speed,           // 速度 km/h
        int altitude,        // 海拔 m
        int satellites,      // 卫星数
        int fuelPercent      // 油量百分比
) {
}
