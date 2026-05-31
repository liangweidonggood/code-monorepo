package com.lwd.netservicenetty.protocol;


/**
 * 注册应答 — 服务端→客户端
 *
 * <p>Body: [流水号(2B)] + [结果(1B)] + [鉴权码(4B BCD)] + [消息长度(1B)] + [消息(N B ASCII)]
 *
 * @author Administrator
 */
public record RegistrationResponse(
        int seqNo,
        int result,
        String authCode,
        String message
) implements TcpPacket {
}
