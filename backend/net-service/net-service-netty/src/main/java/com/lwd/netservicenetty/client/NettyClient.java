package com.lwd.netservicenetty.client;

import com.lwd.netservicenetty.client.simulator.GpsTrackSimulator;
import com.lwd.netservicenetty.core.ProtocolFrameDecoder;
import com.lwd.netservicenetty.core.ProtocolFrameEncoder;
import com.lwd.netservicenetty.protocol.*;
import com.lwd.netservicenetty.transport.Transport;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Netty TCP 客户端 — 模拟车联网终端
 * <p>
 * 生命周期: 连接 → 注册 → 鉴权 → 工作循环(心跳+位置+告警) → 断开重连
 *
 * @author Administrator
 */
@Slf4j
@Component
public class NettyClient {

    private final TcpClientConfig config;
    private final ProtocolFrameEncoder encoder;
    private final ProtocolFrameDecoder decoder;
    private final GpsTrackSimulator simulator;

    private Channel channel;
    private EventLoopGroup group;
    private ScheduledFuture<?> heartbeatTask;
    private ScheduledFuture<?> locationTask;
    private String authCode;
    private int seqNo;
    private int reconnectAttempt;
    private int originalLocationInterval;

    public NettyClient(TcpClientConfig config,
                       ProtocolFrameEncoder encoder,
                       ProtocolFrameDecoder decoder) {
        this.config = config;
        this.encoder = encoder;
        this.decoder = decoder;
        this.simulator = new GpsTrackSimulator(39.9, 116.3, 60.0, 0.0008);
        this.originalLocationInterval = config.locationIntervalSeconds();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("客户端启动，目标: {}:{}", config.host(), config.port());
        connect();
    }

    @PreDestroy
    public void shutdown() {
        log.info("客户端关闭中...");
        cancelTimers();
        if (channel != null && channel.isOpen()) {
            channel.close().syncUninterruptibly();
        }
        if (group != null) {
            group.shutdownGracefully(1, 5, TimeUnit.SECONDS).syncUninterruptibly();
        }
        log.info("客户端已关闭");
    }

    // ── 连接管理 ──

    private void connect() {
        Transport transport = Transport.resolve();
        group = new MultiThreadIoEventLoopGroup(1, transport.factory());

        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(transport.socketChannel())
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(encoder);
                        p.addLast(new LoggingHandler(LogLevel.DEBUG));
                        p.addLast(new IdleStateHandler(0, 0, 0));
                        p.addLast(new LengthFieldBasedFrameDecoder(1024, 1, 1, 3, 0));
                        p.addLast(decoder);
                        p.addLast(new ClientHandler(NettyClient.this));
                    }
                });

        b.connect(config.host(), config.port()).addListener((ChannelFuture f) -> {
            if (f.isSuccess()) {
                channel = f.channel();
                log.info("连接成功: {}", channel.remoteAddress());
                reconnectAttempt = 0;
            } else {
                log.error("连接失败: {}", f.cause().getMessage());
                scheduleReconnect();
            }
        });
    }

    void scheduleReconnect() {
        cancelTimers();
        reconnectAttempt++;
        int delay = Math.min(
                config.reconnectBaseDelaySeconds() * (1 << Math.min(reconnectAttempt - 1, 6)),
                config.reconnectMaxDelaySeconds());
        log.info("{} 秒后第 {} 次重连...", delay, reconnectAttempt);
        if (group != null && !group.isShuttingDown()) {
            group.schedule(this::connect, delay, TimeUnit.SECONDS);
        }
    }

    // ── 协议流程 ──

    void sendRegistration(ChannelHandlerContext ctx) {
        var req = new RegistrationRequest(
                config.terminalId(), config.manufacturerId(), "V1",
                0x10, 0x20, 11, 1, config.licensePlate(), 2);
        ctx.writeAndFlush(req);
    }

    void sendAuth(ChannelHandlerContext ctx) {
        var req = new AuthRequest(config.terminalId(), authCode, nextSeq());
        ctx.writeAndFlush(req);
    }

    void startWorking(ChannelHandlerContext ctx) {
        log.info("工作循环启动 — 心跳:{}s 位置:{}s",
                config.heartbeatIntervalSeconds(), config.locationIntervalSeconds());
        // 定时心跳
        heartbeatTask = ctx.executor().scheduleAtFixedRate(
                () -> sendHeartbeat(ctx),
                config.heartbeatIntervalSeconds(),
                config.heartbeatIntervalSeconds(),
                TimeUnit.SECONDS);
        // 定时位置上报
        locationTask = ctx.executor().scheduleAtFixedRate(
                () -> sendLocation(ctx),
                config.locationIntervalSeconds(),
                config.locationIntervalSeconds(),
                TimeUnit.SECONDS);

        // 首次立刻上报一次位置
        sendLocation(ctx);
    }

    // ── 业务消息发送 ──

    private void sendHeartbeat(ChannelHandlerContext ctx) {
        if (ctx.channel().isActive()) {
            byte flags = (byte) 0x01; // ACC 开
            var hb = new Heartbeat(config.terminalId(), nextSeq(), flags);
            ctx.writeAndFlush(hb);
        }
    }

    private void sendLocation(ChannelHandlerContext ctx) {
        if (!ctx.channel().isActive()) return;

        var point = simulator.nextPosition();
        var loc = new LocationReport(
                config.terminalId(),
                point.latitude(), point.longitude(),
                point.speed(), 90, point.altitude(),
                5000 + seqNo, point.fuelPercent(), 85,
                1250, 28, point.satellites(),
                0, LocalDateTime.now(), nextSeq());
        ctx.writeAndFlush(loc);

        // 随机 5% 概率发送告警
        if (Math.random() < 0.05) {
            var alarm = new AlarmReport(config.terminalId(), 1, 1,
                    point.latitude(), point.longitude(), point.speed(),
                    LocalDateTime.now(), "模拟测试告警");
            ctx.writeAndFlush(alarm);
        }
    }

    // ── 指令响应 ──

    void adjustLocationInterval(int seconds) {
        log.info("调整位置上报间隔: {}s (原: {}s)", seconds, originalLocationInterval);
        // 重新调度
        if (locationTask != null) locationTask.cancel(false);
        if (channel != null && channel.isActive()) {
            locationTask = channel.eventLoop().scheduleAtFixedRate(
                    () -> sendLocation(channel.pipeline().lastContext()),
                    seconds, seconds, TimeUnit.SECONDS);
        }
    }

    void resetLocationInterval() {
        adjustLocationInterval(originalLocationInterval);
    }

    void setAuthCode(String code) {
        this.authCode = code;
    }

    // ── 工具 ──

    private int nextSeq() {
        return ++seqNo;
    }

    private void cancelTimers() {
        if (heartbeatTask != null) heartbeatTask.cancel(false);
        if (locationTask != null) locationTask.cancel(false);
    }
}
