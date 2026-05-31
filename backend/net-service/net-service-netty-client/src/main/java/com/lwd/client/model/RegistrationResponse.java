package com.lwd.client.model;

/**
 * 注册应答 — 服务端返回的注册结果。
 *
 * @author Administrator
 */
public record RegistrationResponse(
        int seqNo,          // 流水号
        int result,         // 结果（0=成功）
        String authCode,    // 鉴权码（8 位 hex）
        String message      // 应答消息（UTF-8）
) {
}
