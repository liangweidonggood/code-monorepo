package com.lwd.client.model;

/**
 * 通用应答 — 服务端/客户端均可使用。
 *
 * @author Administrator
 */
public record CommonResponse(
        int serialNo,       // 流水号
        short respMsgType,  // 应答的消息类型码
        int result          // 结果（0=成功）
) {
}
