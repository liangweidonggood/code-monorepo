package com.lwd.netservicenetty.protocol;


import java.time.LocalDateTime;

/**
 * 告警上报 — 紧急事件通知
 *
 * <p>Body: [终端号(14B BCD)] + [告警类型(2B)] + [告警等级(1B)] + [纬度(4B)] + [经度(4B)]
 * + [速度(2B)] + [告警时间(6B BCD)] + [描述长度(1B)] + [描述(N B ASCII)]
 *
 * @author Administrator
 */
public record AlarmReport(
        String terminalId,
        int alarmType,
        int alarmLevel,
        double latitude,
        double longitude,
        int speed,
        LocalDateTime alarmTime,
        String description
) implements TcpPacket {
}
