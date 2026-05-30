package com.lwd.netservicenetty.protocol;


/**
 * 立即回传指令 — 服务端下发，要求终端上报位置
 * <p>
 * Body: [终端号(14B BCD)] + [流水号(2B)] + [标志(1B)] + [间隔(2B)] + [持续时间(2B)]
 *
 * @author Administrator
 */
public record ImmediateReplayCmd(
        String terminalId,
        int seqNo,
        byte flags,
        int uploadInterval,
        int duration
) implements TcpPacket {
}
