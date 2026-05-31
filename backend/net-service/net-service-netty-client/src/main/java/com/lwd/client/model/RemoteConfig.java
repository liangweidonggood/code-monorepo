package com.lwd.client.model;

/**
 * 远程配置指令 — 服务端下发参数配置。
 *
 * @author Administrator
 */
public record RemoteConfig(
        int seqNo,          // 流水号
        int paramId,        // 参数 ID
        byte[] paramValue   // 参数值（原始字节）
) {
}
