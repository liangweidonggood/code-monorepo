package com.lwd.netservicenetty.server;

import io.netty.handler.logging.LogLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Netty 服务端配置
 *
 * @author Administrator
 */
@ConfigurationProperties(prefix = "netty.server")
public record NettyServerConfig(int port, LogLevel logLevel) {
}
