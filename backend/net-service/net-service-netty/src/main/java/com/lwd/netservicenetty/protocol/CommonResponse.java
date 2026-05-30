package com.lwd.netservicenetty.protocol;


/**
 * 通用应答 — 双向消息，对任意请求的确认/拒绝
 * <p>
 * Body 结构: [流水号(2B)] + [应答消息类型(1B)] + [结果(1B)] = 4 字节
 *
 * @param serialNo     对应请求的流水号
 * @param respMsgType  应答的消息类型
 * @param result       0=成功 1=失败 2=不支持 3=消息有误
 * @author Administrator
 */
public record CommonResponse(
        int serialNo,
        short respMsgType,
        int result
) implements TcpPacket {
}
