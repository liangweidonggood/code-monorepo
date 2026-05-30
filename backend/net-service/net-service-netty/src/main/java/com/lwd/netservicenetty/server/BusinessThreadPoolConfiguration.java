package com.lwd.netservicenetty.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 业务线程池 Bean 配置
 * <p>
 * 弹性伸缩策略：
 * <li>核心线程常驻，峰值扩容到 maxPoolSize</li>
 * <li>有界队列 + CallerRunsPolicy，队列满后调用者线程兜底执行，形成天然背压</li>
 * <li>空闲 60s 后回收多余线程</li>
 *
 * @author Administrator
 */
@Slf4j
@Configuration
public class BusinessThreadPoolConfiguration {

    @Bean("businessExecutor")
    public ThreadPoolTaskExecutor businessExecutor(BusinessThreadPoolConfig config) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.corePoolSize());
        executor.setMaxPoolSize(config.maxPoolSize());
        executor.setQueueCapacity(config.queueCapacity());
        executor.setKeepAliveSeconds(config.keepAliveSeconds());
        executor.setThreadNamePrefix(config.threadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("业务线程池初始化完成 — 核心:{} 最大:{} 队列:{} 空闲回收:{}s",
                config.corePoolSize(), config.maxPoolSize(),
                config.queueCapacity(), config.keepAliveSeconds());
        return executor;
    }
}
