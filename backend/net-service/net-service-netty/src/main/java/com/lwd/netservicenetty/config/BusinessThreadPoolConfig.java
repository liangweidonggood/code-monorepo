package com.lwd.netservicenetty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务线程池配置 — 弹性伸缩，有界队列 + CallerRunsPolicy 天然背压
 *
 * @author Administrator
 */
@ConfigurationProperties(prefix = "netty.server.business-thread-pool")
public record BusinessThreadPoolConfig(
        int corePoolSize,
        int maxPoolSize,
        int queueCapacity,
        int keepAliveSeconds,
        String threadNamePrefix
) {
}
