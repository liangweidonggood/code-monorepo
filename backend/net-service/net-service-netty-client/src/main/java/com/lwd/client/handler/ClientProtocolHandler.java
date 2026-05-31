package com.lwd.client.handler;

import com.lwd.client.model.*;
import com.lwd.client.session.ClientSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.lwd.client.codec.FrameCodec.*;
import static com.lwd.client.codec.ProtocolConstants.*;

/**
 * 入站协议消息分派 + 工作循环调度
 *
 * @author Administrator
 */
@ChannelHandler.Sharable
public class ClientProtocolHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger LOG = LoggerFactory.getLogger(ClientProtocolHandler.class);

    /** 共享会话状态 */
    private final ClientSession session;
    /** 心跳发送器 */
    private final ClientHeartbeatHandler heartbeatHandler;
    /** 位置上报发送器 */
    private final ClientLocationHandler locationHandler;

    /**
     * 构造协议消息分派器。
     *
     * @param session          共享会话状态
     * @param heartbeatHandler 心跳发送器
     * @param locationHandler  位置上报发送器
     */
    public ClientProtocolHandler(ClientSession session,
                                  ClientHeartbeatHandler heartbeatHandler,
                                  ClientLocationHandler locationHandler) {
        super();
        this.session = session;
        this.heartbeatHandler = heartbeatHandler;
        this.locationHandler = locationHandler;
    }

    /** 连接建立后自动发送注册请求 */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOG.info("连接已建立，发送注册请求...");
        RegistrationBody reg = new RegistrationBody(session.config.terminalId(), session.config.manufacturerId(),
                "0001", 0x10, 0x20, 11, 1, session.config.licensePlate(), 2);
        writeFrame(ctx, MSG_REGISTRATION_REQUEST, encodeRegistrationRequest(reg));
    }

    /** 连接断开后触发重连回调 */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOG.warn("连接断开，将触发重连...");
        Runnable cb = session.onDisconnect();
        if (cb != null) {
            cb.run();
        }
    }

    /** 按消息类型分发处理 */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf frame) {
        DecodedFrame decoded = decodeFrame(frame);
        switch (decoded.msgType()) {
          case MSG_REGISTRATION_RESPONSE -> handleReg(ctx, decoded.body());
          case MSG_COMMON_RESPONSE_SERVER, MSG_COMMON_RESPONSE -> handleCommonResp(ctx, decoded.body());
          case MSG_IMMEDIATE_REPLAY -> handleReplay(ctx, decoded.body());
          case MSG_REMOTE_CONFIG -> handleRemoteConfig(ctx, decoded.body());
          default -> {
              if (LOG.isDebugEnabled()) {
                  LOG.debug("未处理: 0x{}", Integer.toHexString(decoded.msgType()));
              }
          }
        }
    }

    /** 异常时关闭连接 */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("协议异常", cause);
        ctx.close().addListener(f -> {
            if (!f.isSuccess()) {
                LOG.error("关闭连接失败", f.cause());
            }
        });
    }

    /** 处理注册应答：保存鉴权码 → 发送鉴权请求 */
    private void handleReg(ChannelHandlerContext ctx, ByteBuf body) {
        RegistrationResponse resp = decodeRegistrationResponse(body);
        if (resp.result() == 0) {
            LOG.info("注册成功！鉴权码: {}", resp.authCode());
            session.authCode(resp.authCode());
            writeFrame(ctx, MSG_AUTH_REQUEST,
                    encodeAuthRequest(session.config.terminalId(), session.authCode(), session.nextSeq()));
        } else {
            LOG.error("注册被拒: {}", resp.message());
            ctx.close().addListener(f -> {
                if (!f.isSuccess()) {
                    LOG.error("关闭连接失败", f.cause());
                }
            });
        }
    }

    /** 处理通用应答：鉴权成功则启动工作循环 */
    private void handleCommonResp(ChannelHandlerContext ctx, ByteBuf body) {
        CommonResponse resp = decodeCommonResponse(body);
        if (resp.result() == 0) {
            if (LOG.isInfoEnabled()) {
                LOG.info("应答成功 — 流水号: {} 应答类型: 0x{}",
                        resp.serialNo(), Integer.toHexString(resp.respMsgType() & 0xFF));
            }
            if (resp.respMsgType() == MSG_AUTH_REQUEST) {
                startWorking(ctx);
            }
        } else {
            LOG.warn("应答失败 — 流水号: {} 结果: {}", resp.serialNo(), resp.result());
        }
    }

    /** 处理立即回传指令：调整上报间隔 + 回复通用应答 */
    private void handleReplay(ChannelHandlerContext ctx, ByteBuf body) {
        ImmediateReplay cmd = decodeImmediateReplay(body);
        LOG.info("收到立即回传指令 — 流水号: {} 间隔: {}s 持续: {}s",
                cmd.seqNo(), cmd.uploadInterval(), cmd.duration());
        locationHandler.adjustInterval(cmd.uploadInterval(), cmd.duration());
        CommonResponse resp = new CommonResponse(cmd.seqNo(), (short) MSG_IMMEDIATE_REPLAY, 0);
        writeFrame(ctx, MSG_COMMON_RESPONSE, encodeCommonResponse(resp));
    }

    /** 处理远程配置指令：打印参数 + 回复通用应答 */
    private void handleRemoteConfig(ChannelHandlerContext ctx, ByteBuf body) {
        RemoteConfig cmd = decodeRemoteConfig(body);
        String value = new String(cmd.paramValue(), java.nio.charset.StandardCharsets.UTF_8);
        LOG.info("收到远程配置指令 — 流水号: {} 参数ID: {} 值: {}", cmd.seqNo(), cmd.paramId(), value);
        CommonResponse resp = new CommonResponse(cmd.seqNo(), (short) MSG_REMOTE_CONFIG, 0);
        writeFrame(ctx, MSG_COMMON_RESPONSE, encodeCommonResponse(resp));
    }

    /** 鉴权成功后启动心跳和位置上报定时任务 */
    private void startWorking(ChannelHandlerContext ctx) {
        LOG.info("工作循环启动 — 心跳:{}s 位置:{}s",
                session.config.heartbeatIntervalSeconds(), session.config.locationIntervalSeconds());
        try (BorrowedExecutor exec = BorrowedExecutor.of(ctx.executor())) {
            exec.scheduleAtFixedRate(
                    () -> heartbeatHandler.send(ctx),
                    session.config.heartbeatIntervalSeconds(), session.config.heartbeatIntervalSeconds(),
                    session::heartbeatTask);
            exec.scheduleAtFixedRate(
                    () -> locationHandler.send(ctx),
                    session.config.locationIntervalSeconds(), session.config.locationIntervalSeconds(),
                    session::locationTask);
        }
        locationHandler.send(ctx);
    }

    /**
     * 将消息体包装为完整帧并写入通道，自动 release body。
     *
     * @param ctx     Netty 通道上下文
     * @param msgType 消息类型码
     * @param body    消息体 ByteBuf（调用后 release）
     */
    public static void writeFrame(ChannelHandlerContext ctx, int msgType, ByteBuf body) {
        try {
            byte[] bytes = new byte[body.readableBytes()];
            body.readBytes(bytes);
            ctx.writeAndFlush(buildFrame(msgType, bytes)).addListener(f -> {
                if (!f.isSuccess()) {
                    LOG.warn("帧发送失败: 0x{}", Integer.toHexString(msgType), f.cause());
                }
            });
        } finally {
            body.release();
        }
    }
}
