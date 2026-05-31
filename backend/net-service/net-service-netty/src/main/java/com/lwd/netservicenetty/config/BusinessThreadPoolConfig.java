package com.lwd.netservicenetty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务线程池配置 — 弹性伸缩，有界队列 + CallerRunsPolicy 天然背压
 *
 * @author Administrator
 */
@ConfigurationProperties(prefix = "netty.server.business-thread-pool")
public record BusinessThreadPoolConfig(
        int corePoolSize,        // 核心线程数
        int maxPoolSize,         // 最大线程数
        int queueCapacity,       // 有界队列容量
        int keepAliveSeconds,    // 空闲线程存活秒数
        String threadNamePrefix  // 线程名前缀
) {
}
