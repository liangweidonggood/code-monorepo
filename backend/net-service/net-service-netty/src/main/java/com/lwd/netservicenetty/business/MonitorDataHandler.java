package com.lwd.netservicenetty.business;

import com.lwd.netservicenetty.protocol.TcpPacket;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 业务终点站 — 接收解码后的 TcpPacket，按类型分发处理
 * <p>
 * SimpleChannelInboundHandler 自动 release ByteBuf/ByteBufHolder。
 * 上游 ProtocolFrameDecoder 已将 ByteBuf 解码为 TcpPacket。
 *
 * @author Administrator
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class MonitorDataHandler extends SimpleChannelInboundHandler<TcpPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpPacket packet) {
        log.info("【收到消息】类型: {}", packet.getClass().getSimpleName());

        switch (packet) {
            case com.lwd.netservicenetty.protocol.RegistrationRequest req ->
                    handleRegistration(ctx, req);
            case com.lwd.netservicenetty.protocol.AuthRequest req ->
                    handleAuth(ctx, req);
            case com.lwd.netservicenetty.protocol.Heartbeat hb ->
                    handleHeartbeat(ctx, hb);
            case com.lwd.netservicenetty.protocol.LocationReport loc ->
                    handleLocation(ctx, loc);
            case com.lwd.netservicenetty.protocol.AlarmReport alarm ->
                    handleAlarm(ctx, alarm);
            case com.lwd.netservicenetty.protocol.CommonResponse resp ->
                    handleCommonResponse(ctx, resp);
            default ->
                    log.warn("未处理的报文类型: {}", packet.getClass().getSimpleName());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("通道 [{}] 发生未处理异常，关闭连接", ctx.channel().id(), cause);
        ctx.close();
    }

    // ── 业务处理器 ──

    private void handleRegistration(ChannelHandlerContext ctx, com.lwd.netservicenetty.protocol.RegistrationRequest req) {
        log.info("【注册】终端: {} 厂商: {} 设备型号: {} 车牌: {}",
                req.terminalId(), req.manufacturerId(), req.terminalModel(), req.licensePlate());
        // TODO: 持久化 + 生成鉴权码，回复 RegistrationResponse
    }

    private void handleAuth(ChannelHandlerContext ctx, com.lwd.netservicenetty.protocol.AuthRequest req) {
        log.info("【鉴权】终端: {} 鉴权码: {} 流水号: {}", req.terminalId(), req.authCode(), req.seqNo());
        // TODO: 验证鉴权码，发送 CommonResponse
    }

    private void handleHeartbeat(ChannelHandlerContext ctx, com.lwd.netservicenetty.protocol.Heartbeat hb) {
        log.debug("【心跳】终端: {} 流水号: {} 状态: 0x{}",
                hb.terminalId(), hb.seqNo(), Integer.toHexString(hb.statusFlags() & 0xFF));
    }

    private void handleLocation(ChannelHandlerContext ctx, com.lwd.netservicenetty.protocol.LocationReport loc) {
        log.info("【位置】终端: {} 经纬度: ({}, {}) 速度: {}km/h 方向: {}° 里程: {}km 油量: {}% 卫星: {}",
                loc.terminalId(), loc.latitude(), loc.longitude(),
                loc.speed(), loc.direction(), loc.mileage(),
                loc.fuelPercent(), loc.satellites());
        // TODO: 持久化位置数据
    }

    private void handleAlarm(ChannelHandlerContext ctx, com.lwd.netservicenetty.protocol.AlarmReport alarm) {
        log.warn("【告警!!】终端: {} 类型: {} 等级: {} 位置: ({}, {}) 描述: {}",
                alarm.terminalId(), alarm.alarmType(), alarm.alarmLevel(),
                alarm.latitude(), alarm.longitude(), alarm.description());
        // TODO: 推送告警到监控中心
    }

    private void handleCommonResponse(ChannelHandlerContext ctx, com.lwd.netservicenetty.protocol.CommonResponse resp) {
        log.info("【应答】流水号: {} 应答类型: 0x{} 结果: {}",
                resp.serialNo(), Integer.toHexString(resp.respMsgType() & 0xFF), resp.result());
    }
}
