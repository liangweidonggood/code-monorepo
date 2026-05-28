package com.lwd.netservicenetty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * TcpServer
 *
 * @author Administrator
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TcpServer {

    private final MyChannelInitializer myChannelInitializer;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    /**
     * 开启服务
     */
    @EventListener(ApplicationReadyEvent.class)
    private void start() {
        log.info("准备开启tcp服务...");
        // 1. 使用通用的多线程 I/O 事件循环组，通过工厂把 NIO 的行为注入进去
        bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128) // 连接队列
                .childOption(ChannelOption.SO_KEEPALIVE, true) // 保持长连接
                .childHandler(myChannelInitializer);
        bootstrap.bind(8888).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                serverChannel = future.channel();
                log.info("Netty tcp server 启动成功！");
            } else {
                log.error("Netty 启动失败: {}", String.valueOf(future.cause()));
                shutdown();
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        log.info("收到 Spring 关闭信号，开始优雅释放 Netty 资源...");
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Netty 优雅停机完成。");
    }
}
