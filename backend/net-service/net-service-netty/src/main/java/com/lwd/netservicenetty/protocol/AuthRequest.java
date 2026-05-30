package com.lwd.netservicenetty.protocol;


/**
 * 鉴权请求
 * <p>
 * Body: [终端号(14B BCD)] + [鉴权码(4B BCD)] + [流水号(2B)] = 20 字节
 *
 * @author Administrator
 */
public record AuthRequest(
        String terminalId,
        String authCode,
        int seqNo
) implements TcpPacket {
}
