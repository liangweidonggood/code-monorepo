package com.lwd.client.model;

import java.time.LocalDateTime;

/**
 * 告警上报消息体 — 包含告警类型、等级、位置、描述。
 *
 * @author Administrator
 */
public record AlarmBody(
        String terminalId,          // 终端号（14 位数字，BCD 编码）
        int alarmType,              // 告警类型
        int alarmLevel,             // 告警等级
        double lat,                 // 纬度
        double lng,                 // 经度
        int speed,                  // 速度 km/h
        LocalDateTime alarmTime,    // 告警时间
        String description          // 告警描述（UTF-8）
) {
}
