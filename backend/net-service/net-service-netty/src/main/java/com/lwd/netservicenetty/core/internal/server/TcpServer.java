package com.lwd.netservicenetty.core.internal.server;

import com.lwd.netservicenetty.config.NettyServerConfig;
import com.lwd.netservicenetty.transport.Transport;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.WriteBufferWaterMark;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * TcpServer — Netty TCP 服务端生命周期管理
 *
 * @author Administrator
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TcpServer {

    /** 低水位 512KB，高水位 1MB */
    private static final int LOW_WATER_MARK = 512 * 1024;
    private static final int HIGH_WATER_MARK = 1024 * 1024;

    private final ChannelInitializer channelInitializer;
    private final NettyServerConfig config;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    /**
     * 应用就绪后自动开启 TCP 服务
     */
    @EventListener(ApplicationReadyEvent.class)
    private void start() {
        log.info("准备开启tcp服务...");
        Transport transport = Transport.resolve();
        IoHandlerFactory ioHandlerFactory = transport.factory();
        Class<? extends ServerChannel> channelClass = transport.channel();
        int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
        bossGroup = new MultiThreadIoEventLoopGroup(1, ioHandlerFactory);
        workerGroup = new MultiThreadIoEventLoopGroup(workerThreads, ioHandlerFactory);
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(channelClass)
                // 已完成三次握手但未被 accept 的连接队列大小
                .option(ChannelOption.SO_BACKLOG, 1024)
                // 允许快速重启时复用 TIME_WAIT 状态的端口
                .option(ChannelOption.SO_REUSEADDR, true)
                // 启用 TCP KeepAlive 探测，及时清理死连接
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                // 禁用 Nagle 算法，小数据包立即发送不等待合并
                .childOption(ChannelOption.TCP_NODELAY, true)
                // 堆外内存池，减少 GC 压力
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                // 高低水位线：低 512KB 触发可写恢复，高 1MB 触发背压
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        new WriteBufferWaterMark(LOW_WATER_MARK, HIGH_WATER_MARK))
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
            // 优雅停机：2 秒静默期，15 秒超时
            if (bossGroup != null) {
                bossGroup.shutdownGracefully(2, 15, TimeUnit.SECONDS).syncUninterruptibly();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully(2, 15, TimeUnit.SECONDS).syncUninterruptibly();
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
