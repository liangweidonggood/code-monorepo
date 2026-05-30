package com.lwd.netservicenetty.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 客户端配置
 *
 * @author Administrator
 */
@ConfigurationProperties(prefix = "netty.client")
public record TcpClientConfig(
        boolean enabled,
        String host,
        int port,
        int heartbeatIntervalSeconds,
        int locationIntervalSeconds,
        int reconnectBaseDelaySeconds,
        int reconnectMaxDelaySeconds,
        String terminalId,
        int manufacturerId,
        String licensePlate
) {
}
