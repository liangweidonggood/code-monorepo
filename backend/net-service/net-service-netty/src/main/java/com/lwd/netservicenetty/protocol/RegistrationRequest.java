package com.lwd.netservicenetty.protocol;


/**
 * 注册请求
 * <p>
 * Body: [终端号(7B BCD)] + [厂商编号(2B)] + [设备型号(2B BCD)] + [硬件版本(1B)]
 * + [软件版本(1B)] + [省域ID(2B)] + [市县域ID(2B)] + [车牌长度(1B)] + [车牌号(N B ASCII)]
 * + [车牌颜色(1B)] = 19 + plateLen 字节
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
