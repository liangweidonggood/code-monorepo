package com.lwd.netservicenetty.protocol;


/**
 * 数据透传 — 双向自定义业务数据
 * <p>
 * Body: [终端号(14B BCD)] + [数据类型(2B)] + [数据长度(2B)] + [透传数据(N B)]
 *
 * @author Administrator
 */
public record DataTransmission(
        String terminalId,
        int dataType,
        byte[] payload
) implements TcpPacket {
}
