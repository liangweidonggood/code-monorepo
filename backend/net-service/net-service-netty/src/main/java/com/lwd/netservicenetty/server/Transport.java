package com.lwd.netservicenetty.server;

import io.netty.channel.IoHandlerFactory;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty 传输层选择，按 io_uring > Epoll > NIO 优先级自动降级
 * @author Administrator
 */
@Slf4j
public enum Transport {

    /** Linux io_uring（内核 5.10+），零系统调用，最高吞吐 */
    IO_URING {
        @Override
        public IoHandlerFactory factory() { return IoUringIoHandler.newFactory(); }
        @Override
        public Class<? extends ServerChannel> channel() { return IoUringServerSocketChannel.class; }
    },
    /** Linux Epoll，传统高性能方案 */
    EPOLL {
        @Override
        public IoHandlerFactory factory() { return EpollIoHandler.newFactory(); }
        @Override
        public Class<? extends ServerChannel> channel() { return EpollServerSocketChannel.class; }
    },
    /** 通用 NIO，全平台兜底 */
    NIO {
        @Override
        public IoHandlerFactory factory() { return NioIoHandler.newFactory(); }
        @Override
        public Class<? extends ServerChannel> channel() { return NioServerSocketChannel.class; }
    };

    public abstract IoHandlerFactory factory();

    public abstract Class<? extends ServerChannel> channel();

    public static Transport resolve() {
        if (IoUring.isAvailable()) {
            log.info("使用 io_uring 传输");
            return IO_URING;
        }
        if (Epoll.isAvailable()) {
            log.info("使用 Epoll 传输");
            return EPOLL;
        }
        log.info("使用 NIO 传输");
        return NIO;
    }
}
