package com.lwd.netservicenetty.config;

import io.netty.handler.logging.LogLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Netty 服务端配置
 *
 * @author Administrator
 */
@ConfigurationProperties(prefix = "netty.server")
public record NettyServerConfig(
        int port,            // 服务端 TCP 监听端口
        LogLevel logLevel    // Netty 日志级别
) {
}
