package com.lwd.client.handler;

import com.lwd.client.model.*;
import com.lwd.client.session.ClientSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import static com.lwd.client.codec.FrameCodec.*;
import static com.lwd.client.codec.ProtocolConstants.*;

/**
 * 位置上报发送器 + 随机告警 + 响应服务端指令调整间隔
 *
 * @author Administrator
 */
@ChannelHandler.Sharable
public class ClientLocationHandler {

    /** 模拟告警触发概率：5% */
    private static final double SIMULATED_ALARM_RATE = 0.05;
    /** 安全随机数生成器 */
    private static final SecureRandom RNG = new SecureRandom();
    /** 共享会话状态 */
    private final ClientSession session;

    /**
     * 构造位置上报发送器。
     *
     * @param session 共享会话状态
     */
    public ClientLocationHandler(ClientSession session) {
        this.session = session;
    }

    /** 发送位置上报（附带 5% 概率随机告警） */
    public void send(ChannelHandlerContext ctx) {
        if (!ctx.channel().isActive()) {
            return;
        }
        GpsPoint point = session.simulator.nextPosition();
        LocationBody loc = new LocationBody(session.config.terminalId(),
                point.latitude(), point.longitude(), point.speed(), 90, point.altitude(),
                5000 + session.nextSeq(), point.fuelPercent(), 85, 1250, 28, point.satellites(),
                0, LocalDateTime.now(), session.nextSeq());
        ClientProtocolHandler.writeFrame(ctx, MSG_LOCATION_REPORT, encodeLocationReport(loc));
        if (RNG.nextDouble() < SIMULATED_ALARM_RATE) {
            AlarmBody alarm = new AlarmBody(session.config.terminalId(), 1, 1,
                    point.latitude(), point.longitude(), point.speed(), LocalDateTime.now(), "模拟测试告警");
            ClientProtocolHandler.writeFrame(ctx, MSG_ALARM_REPORT, encodeAlarmReport(alarm));
        }
    }

    /**
     * 按服务端指令调整上报间隔，到期自动恢复为配置值。
     * EventLoop 由 Channel 管理生命周期，此处无需 close。
     */
    public void adjustInterval(int seconds, int durationSeconds) {
        session.cancelLocationTask();
        Channel ch = session.channel();
        if (ch != null && ch.isActive()) {
            try (BorrowedExecutor exec = BorrowedExecutor.of(ch.eventLoop())) {
                exec.scheduleAtFixedRate(
                        () -> send(ch.pipeline().lastContext()),
                        seconds, seconds,
                        session::locationTask);
                exec.schedule(
                        () -> adjustInterval(session.config.locationIntervalSeconds(), 0),
                        durationSeconds);
            }
        }
    }
}
