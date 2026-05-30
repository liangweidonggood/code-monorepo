package com.lwd.netservicenetty.core.internal.server;

import com.lwd.netservicenetty.config.NettyServerConfig;
import com.lwd.netservicenetty.transport.Transport;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * TcpServer
 *
 * @author Administrator
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TcpServer {

    private static final int BACKLOG_SIZE = 1024;
    private static final int LOW_WATER_MARK_BYTES = 512 * 1024;
    private static final int HIGH_WATER_MARK_BYTES = 1024 * 1024;
    private static final int BOSS_THREADS = 1;
    private static final int SHUTDOWN_QUIET_SECONDS = 2;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 15;

    private final ChannelInitializer channelInitializer;
    private final NettyServerConfig config;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    /**
     * 开启服务
     */
    @EventListener(ApplicationReadyEvent.class)
    private void start() {
        log.info("准备开启tcp服务...");
        Transport transport = Transport.resolve();
        IoHandlerFactory ioHandlerFactory = transport.factory();
        Class<? extends ServerChannel> channelClass = transport.channel();
        int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
        bossGroup = new MultiThreadIoEventLoopGroup(BOSS_THREADS, ioHandlerFactory);
        workerGroup = new MultiThreadIoEventLoopGroup(workerThreads, ioHandlerFactory);
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(channelClass)
                .option(ChannelOption.SO_BACKLOG, BACKLOG_SIZE)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        new WriteBufferWaterMark(LOW_WATER_MARK_BYTES, HIGH_WATER_MARK_BYTES))
                .childHandler(channelInitializer);
        ChannelFuture bindFuture = b.bind(config.port()).awaitUninterruptibly();
        if (bindFuture.isSuccess()) {
            serverChannel = bindFuture.channel();
            log.info("Netty tcp server 启动成功！");
        } else {
            log.error("Netty 启动失败", bindFuture.cause());
            releaseResourcesAsync();
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("收到 Spring 关闭信号，开始释放 Netty 资源...");
        if (serverChannel != null && serverChannel.isOpen()) {
            serverChannel.close().syncUninterruptibly();
        }
        try {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully(SHUTDOWN_QUIET_SECONDS, SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS).syncUninterruptibly();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully(SHUTDOWN_QUIET_SECONDS, SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS).syncUninterruptibly();
            }
        } catch (Exception e) {
            log.error("Netty 释放资源期间遭遇异常", e);
        }
        log.info("Netty 优雅停机完成。");
    }

    /**
     * 启动失败时异步释放资源，不得在 Netty I/O 线程内调用 sync/syncUninterruptibly
     */
    private void releaseResourcesAsync() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
