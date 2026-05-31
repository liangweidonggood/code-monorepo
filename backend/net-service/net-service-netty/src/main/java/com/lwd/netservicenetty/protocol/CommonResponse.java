package com.lwd.netservicenetty.protocol;


/**
 * 通用应答 — 对任意请求的确认/拒绝。Body: [流水号(2B)] [应答类型(1B)] [结果(1B)] = 4 字节。
 *
 * @author Administrator
 */
public record CommonResponse(
        int serialNo,
        short respMsgType,
        int result
) implements TcpPacket {
}
