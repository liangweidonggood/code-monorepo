package com.lwd.netservicenetty.protocol;


/**
 * 注册请求 — 终端信息 + 车牌号等字段。
 *
 * @author Administrator
 */
public record RegistrationRequest(
        String terminalId,
        int manufacturerId,
        String terminalModel,
        int hardwareVersion,
        int softwareVersion,
        int provinceId,
        int cityId,
        String licensePlate,
        int color
) implements TcpPacket {
}
