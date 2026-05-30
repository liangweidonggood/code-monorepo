package com.lwd.netservicenetty.client;

import com.lwd.netservicenetty.client.simulator.GpsTrackSimulator;
import com.lwd.netservicenetty.core.ProtocolFrameDecoder;
import com.lwd.netservicenetty.core.ProtocolFrameEncoder;
import com.lwd.netservicenetty.protocol.AlarmReport;
import com.lwd.netservicenetty.protocol.AuthRequest;
import com.lwd.netservicenetty.protocol.Heartbeat;
import com.lwd.netservicenetty.protocol.LocationReport;
import com.lwd.netservicenetty.protocol.ProtocolConstants;
import com.lwd.netservicenetty.protocol.RegistrationRequest;
import com.lwd.netservicenetty.transport.Transport;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Netty TCP 客户端 — 模拟车联网终端
 *
 * @author Administrator
 */
@Slf4j
@Component
public class NettyClient {

    /** 心跳状态位：ACC 开 */
    private static final byte HEARTBEAT_ACC_ON = (byte) 0x01;

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
        this.simulator = new GpsTrackSimulator(39.9, 116.3, 60.0, 0.0008);  // 北京天安门起始，60km/h，每次约89m;
        this.originalLocationInterval = config.locationIntervalSeconds();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!config.enabled()) {
            log.info("客户端已禁用");
            return;
        }
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
                        p.addLast(new LengthFieldBasedFrameDecoder(
                                ProtocolConstants.MAX_FRAME_LENGTH,
                                ProtocolConstants.LENGTH_FIELD_OFFSET,
                                ProtocolConstants.LENGTH_FIELD_LENGTH,
                                ProtocolConstants.LENGTH_ADJUSTMENT, 0));
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
        heartbeatTask = ctx.executor().scheduleAtFixedRate(
                () -> sendHeartbeat(ctx),
                config.heartbeatIntervalSeconds(),
                config.heartbeatIntervalSeconds(),
                TimeUnit.SECONDS);
        locationTask = ctx.executor().scheduleAtFixedRate(
                () -> sendLocation(ctx),
                config.locationIntervalSeconds(),
                config.locationIntervalSeconds(),
                TimeUnit.SECONDS);
        sendLocation(ctx);
    }

    private void sendHeartbeat(ChannelHandlerContext ctx) {
        if (ctx.channel().isActive()) {
            var hb = new Heartbeat(config.terminalId(), nextSeq(), HEARTBEAT_ACC_ON);
            ctx.writeAndFlush(hb);
        }
    }

    private void sendLocation(ChannelHandlerContext ctx) {
        if (!ctx.channel().isActive()) {
            return;
        }

        var point = simulator.nextPosition();
        var loc = new LocationReport(
                config.terminalId(),
                point.latitude(), point.longitude(),
                point.speed(), 90, point.altitude(),
                5000 + seqNo, point.fuelPercent(), 85,
                1250, 28, point.satellites(),
                0, LocalDateTime.now(), nextSeq());
        ctx.writeAndFlush(loc);

        if (ThreadLocalRandom.current().nextDouble() < 0.05) {
            var alarm = new AlarmReport(config.terminalId(), 1, 1,
                    point.latitude(), point.longitude(), point.speed(),
                    LocalDateTime.now(), "模拟测试告警");
            ctx.writeAndFlush(alarm);
        }
    }

    void adjustLocationInterval(int seconds) {
        log.info("调整位置上报间隔: {}s (原: {}s)", seconds, originalLocationInterval);
        if (locationTask != null) {
            locationTask.cancel(false);
        }
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

    private int nextSeq() {
        return ++seqNo;
    }

    private void cancelTimers() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
        if (locationTask != null) {
            locationTask.cancel(false);
        }
    }
}
