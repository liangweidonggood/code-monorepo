package com.lwd.netservicenetty.business;

import com.lwd.netservicenetty.protocol.TcpPacket;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 业务终点站 — 接收解码后的 TcpPacket，按类型分发处理。
 *
 *
 * <p>SimpleChannelInboundHandler 自动 release ByteBuf/ByteBufHolder。
 * 上游 ProtocolFrameDecoder 已将 ByteBuf 解码为 TcpPacket。</p>
 *
 * @author Administrator
 */
@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class MonitorDataHandler extends SimpleChannelInboundHandler<TcpPacket> {

    /** 预置鉴权码，客户端注册成功后凭此码鉴权 */
    private static final String STATIC_AUTH_CODE = "01234567";

    private final DeviceRegistry deviceRegistry;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        deviceRegistry.unregister(ctx);
        ctx.fireChannelInactive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpPacket packet) {
        log.info("【收到消息】类型: {}", packet.getClass().getSimpleName());

        switch (packet) {
          case com.lwd.netservicenetty.protocol.RegistrationRequest req ->
              handleRegistration(req, ctx);
          case com.lwd.netservicenetty.protocol.AuthRequest req ->
              handleAuth(req, ctx);
          case com.lwd.netservicenetty.protocol.Heartbeat hb ->
              handleHeartbeat(hb);
          case com.lwd.netservicenetty.protocol.LocationReport loc ->
              handleLocation(loc);
          case com.lwd.netservicenetty.protocol.AlarmReport alarm ->
              handleAlarm(alarm);
          case com.lwd.netservicenetty.protocol.CommonResponse resp ->
              handleCommonResponse(resp);
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

    private void handleRegistration(com.lwd.netservicenetty.protocol.RegistrationRequest req,
                                    ChannelHandlerContext ctx) {
        log.info("【注册】终端: {} 厂商: {} 设备型号: {} 车牌: {}",
                req.terminalId(), req.manufacturerId(), req.terminalModel(), req.licensePlate());
        com.lwd.netservicenetty.protocol.RegistrationResponse resp =
                new com.lwd.netservicenetty.protocol.RegistrationResponse(
                        req.provinceId(), 0, STATIC_AUTH_CODE, "注册成功");
        ctx.writeAndFlush(resp).addListener(f -> {
            if (!f.isSuccess()) {
                log.error("注册应答发送失败", f.cause());
            }
        });
        log.info("【注册应答】已发送 → 终端: {}", req.terminalId());
    }

    private void handleAuth(com.lwd.netservicenetty.protocol.AuthRequest req,
                            ChannelHandlerContext ctx) {
        log.info("【鉴权】终端: {} 鉴权码: {} 流水号: {}", req.terminalId(), req.authCode(), req.seqNo());
        com.lwd.netservicenetty.protocol.CommonResponse resp = new com.lwd.netservicenetty.protocol.CommonResponse(
                req.seqNo(), (short) com.lwd.netservicenetty.protocol.ProtocolConstants.MSG_AUTH_REQUEST,
                STATIC_AUTH_CODE.equals(req.authCode()) ? 0 : 1);
        ctx.writeAndFlush(resp).addListener(f -> {
            if (!f.isSuccess()) {
                log.error("鉴权应答发送失败", f.cause());
            }
        });
        log.info("【鉴权应答】已发送 → 终端: {} 结果: {}", req.terminalId(),
                STATIC_AUTH_CODE.equals(req.authCode()) ? "成功" : "失败");
        deviceRegistry.register(req.terminalId(), ctx);
    }

    private void handleHeartbeat(com.lwd.netservicenetty.protocol.Heartbeat hb) {
        log.debug("【心跳】终端: {} 流水号: {} 状态: 0x{}",
                hb.terminalId(), hb.seqNo(), Integer.toHexString(hb.statusFlags() & 0xFF));
    }

    private void handleLocation(com.lwd.netservicenetty.protocol.LocationReport loc) {
        log.info("【位置】终端: {} 经纬度: ({}, {}) 速度: {}km/h 方向: {}° 里程: {}km 油量: {}% 卫星: {}",
                loc.terminalId(), loc.latitude(), loc.longitude(),
                loc.speed(), loc.direction(), loc.mileage(),
                loc.fuelPercent(), loc.satellites());
        // 规划: 持久化位置数据
    }

    private void handleAlarm(com.lwd.netservicenetty.protocol.AlarmReport alarm) {
        log.warn("【告警!!】终端: {} 类型: {} 等级: {} 位置: ({}, {}) 描述: {}",
                alarm.terminalId(), alarm.alarmType(), alarm.alarmLevel(),
                alarm.latitude(), alarm.longitude(), alarm.description());
        // 规划: 推送告警到监控中心
    }

    private void handleCommonResponse(com.lwd.netservicenetty.protocol.CommonResponse resp) {
        log.info("【应答】流水号: {} 应答类型: 0x{} 结果: {}",
                resp.serialNo(), Integer.toHexString(resp.respMsgType() & 0xFF), resp.result());
    }
}
