package com.lwd.netservicenetty.protocol;


/**
 * 心跳 — 链路保活
 *
 * <p>Body: [终端号(14B BCD)] + [流水号(2B)] + [状态位(1B)] = 17 字节
 *
 * @author Administrator
 */
public record Heartbeat(
        String terminalId,
        int seqNo,
        byte statusFlags
) implements TcpPacket {
}
