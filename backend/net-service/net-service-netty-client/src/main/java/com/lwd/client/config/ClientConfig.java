package com.lwd.client.config;

/**
 * 客户端配置。
 *
 * @author Administrator
 */
public record ClientConfig(
        String host,                     // 服务端主机地址
        int port,                        // 服务端 TCP 端口
        String terminalId,               // 终端号（14 位数字）
        int manufacturerId,              // 厂商编号
        String licensePlate,             // 车牌号（UTF-8）
        int heartbeatIntervalSeconds,    // 心跳间隔秒数
        int locationIntervalSeconds,     // 位置上报间隔秒数
        int reconnectBaseDelaySeconds,   // 重连基础延迟秒数
        int reconnectMaxDelaySeconds     // 重连最大延迟秒数
) {
}
