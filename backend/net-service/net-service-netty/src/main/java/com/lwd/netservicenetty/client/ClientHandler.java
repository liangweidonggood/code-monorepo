package com.lwd.netservicenetty.client;

import com.lwd.netservicenetty.protocol.CommonResponse;
import com.lwd.netservicenetty.protocol.DataTransmission;
import com.lwd.netservicenetty.protocol.ImmediateReplayCmd;
import com.lwd.netservicenetty.protocol.ProtocolConstants;
import com.lwd.netservicenetty.protocol.RegistrationResponse;
import com.lwd.netservicenetty.protocol.RemoteConfigCmd;
import com.lwd.netservicenetty.protocol.TcpPacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 客户端消息处理器 — 接收服务端下发的消息并响应
 *
 * @author Administrator
 */
@Slf4j
@ChannelHandler.Sharable
public class ClientHandler extends SimpleChannelInboundHandler<TcpPacket> {

    private final NettyClient client;

    public ClientHandler(NettyClient client) {
        this.client = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpPacket packet) {
        switch (packet) {
            case RegistrationResponse resp -> handleRegistrationResponse(ctx, resp);
            case CommonResponse resp -> handleCommonResponse(ctx, resp);
            case ImmediateReplayCmd cmd -> handleImmediateReplay(ctx, cmd);
            case RemoteConfigCmd cmd -> handleRemoteConfig(ctx, cmd);
            case DataTransmission data -> handleDataDispatch(ctx, data);
            default -> log.debug("客户端未处理的消息: {}", packet.getClass().getSimpleName());
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("连接已建立，发送注册请求...");
        client.sendRegistration(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("连接断开，将触发重连...");
        client.scheduleReconnect();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("客户端异常", cause);
        ctx.close();
    }

    // ── 消息处理 ──

    private void handleRegistrationResponse(ChannelHandlerContext ctx, RegistrationResponse resp) {
        if (resp.result() == 0) {
            log.info("注册成功！鉴权码: {}", resp.authCode());
            client.setAuthCode(resp.authCode());
            client.sendAuth(ctx);
        } else {
            log.error("注册被拒: {}", resp.message());
            ctx.close();
        }
    }

    private void handleCommonResponse(ChannelHandlerContext ctx, CommonResponse resp) {
        if (resp.result() == 0) {
            log.info("应答成功 — 流水号: {} 应答类型: 0x{}", resp.serialNo(),
                    Integer.toHexString(resp.respMsgType() & 0xFF));
            if (resp.respMsgType() == ProtocolConstants.MSG_AUTH_REQUEST) {
                // 鉴权应答成功，开始工作循环
                client.startWorking(ctx);
            }
        } else {
            log.warn("应答失败 — 流水号: {} 结果: {}", resp.serialNo(), resp.result());
        }
    }

    private void handleImmediateReplay(ChannelHandlerContext ctx, ImmediateReplayCmd cmd) {
        log.info("收到立即回传指令 — 间隔: {}s 持续: {}s", cmd.uploadInterval(), cmd.duration());
        client.adjustLocationInterval(cmd.uploadInterval());
        if (cmd.duration() > 0) {
            ctx.executor().schedule(
                    () -> client.resetLocationInterval(),
                    cmd.duration(), TimeUnit.SECONDS);
        }
    }

    private void handleRemoteConfig(ChannelHandlerContext ctx, RemoteConfigCmd cmd) {
        log.info("收到远程配置 — 参数ID: {} 值长度: {}", cmd.paramId(), cmd.paramValue().length);
        var resp = new CommonResponse(cmd.seqNo(), (short) ProtocolConstants.MSG_REMOTE_CONFIG, 0);
        ctx.writeAndFlush(resp);
    }

    private void handleDataDispatch(ChannelHandlerContext ctx, DataTransmission data) {
        log.info("收到数据下发 — 类型: {} 长度: {}", data.dataType(), data.payload().length);
    }
}
