package com.lwd.netservicenetty.protocol;


/**
 * 远程配置指令 — 服务端下发参数修改
 * <p>
 * Body: [终端号(14B BCD)] + [流水号(2B)] + [参数ID(1B)] + [参数长度(1B)] + [参数值(N B)]
 *
 * @author Administrator
 */
public record RemoteConfigCmd(
        String terminalId,
        int seqNo,
        int paramId,
        byte[] paramValue
) implements TcpPacket {
}
