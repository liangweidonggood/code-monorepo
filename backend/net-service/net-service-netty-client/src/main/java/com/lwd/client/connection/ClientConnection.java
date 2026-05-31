package com.lwd.client.connection;

import com.lwd.client.config.ClientConfig;
import com.lwd.client.handler.ClientProtocolHandler;
import com.lwd.client.session.ClientSession;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 连接生命周期管理 — 建立连接、指数退避重连、优雅关闭
 *
 * @author Administrator
 */
public class ClientConnection implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ClientConnection.class);
    /** 协议帧最大长度 */
    private static final int MAX_FRAME_LENGTH = 1024;
    /** 长度字段偏移（跳过魔数） */
    private static final int LENGTH_FIELD_OFFSET = 1;
    /** 长度字段占用字节数 */
    private static final int LENGTH_FIELD_LENGTH = 1;
    /** 帧尾修正值 type(1)+sub(1)+BCC(1)=3 */
    private static final int LENGTH_ADJUSTMENT = 3;

    /** 共享会话状态 */
    private final ClientSession session;
    /** 协议消息分派器 */
    private final ClientProtocolHandler protocolHandler;
    /** Netty 事件循环组（构造时创建，close 时释放） */
    private final EventLoopGroup group;

    /**
     * 构造连接管理器。
     *
     * @param session         共享会话状态
     * @param protocolHandler 入站协议处理器
     */
    public ClientConnection(ClientSession session, ClientProtocolHandler protocolHandler) {
        this.session = session;
        this.protocolHandler = protocolHandler;
        this.group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        session.group(this.group);
    }

    /** 创建 Bootstrap 并异步连接 */
    public void connect() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline()
                                .addLast(new LoggingHandler(LogLevel.DEBUG))
                                .addLast(new LengthFieldBasedFrameDecoder(
                                        MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET,
                                        LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, 0))
                                .addLast(protocolHandler);
                    }
                });
        ClientConfig config = session.config;
        bootstrap.connect(config.host(), config.port()).addListener((ChannelFuture f) -> {
            if (f.isSuccess()) {
                session.channel(f.channel());
                LOG.info("连接成功: {}", f.channel().remoteAddress());
                session.resetReconnectAttempt();
            } else {
                LOG.error("连接失败: {}", f.cause().getMessage());
                scheduleReconnect();
            }
        });
    }

    /** 指数退避重连：delay = baseDelay * 2^(attempt-1) 上限 maxDelay */
    public void scheduleReconnect() {
        cancelTimers();
        session.incrementReconnectAttempt();
        int attempt = session.reconnectAttempt();
        int delay = Math.min(
                session.config.reconnectBaseDelaySeconds() * (1 << Math.min(attempt - 1, 6)),
                session.config.reconnectMaxDelaySeconds());
        LOG.info("{} 秒后第 {} 次重连...", delay, attempt);
        if (!group.isShuttingDown()) {
            group.schedule(this::connect, delay, TimeUnit.SECONDS);
        }
    }

    /** 优雅关闭 — 取消定时器、关闭连接、释放 EventLoopGroup */
    @Override
    public void close() {
        LOG.info("客户端关闭中...");
        cancelTimers();
        if (session.channel() != null && session.channel().isOpen()) {
            session.channel().close().syncUninterruptibly();
        }
        group.shutdownGracefully(1, 5, TimeUnit.SECONDS).syncUninterruptibly();
        LOG.info("客户端已关闭");
    }

    /** 取消心跳和位置上报告定时任务 */
    private void cancelTimers() {
        session.cancelHeartbeatTask();
        session.cancelLocationTask();
    }
}
