package com.lwd.client.model;

/**
 * 立即回传指令 — 服务端下发，要求终端以指定间隔上报位置。
 *
 * @author Administrator
 */
public record ImmediateReplay(
        int seqNo,           // 流水号
        byte flags,          // 标志位
        int uploadInterval,  // 上报间隔秒数
        int duration         // 持续时间秒数
) {
}
