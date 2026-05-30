package com.lwd.netservicenetty.core.internal.server;

import com.lwd.netservicenetty.business.MonitorDataHandler;
import com.lwd.netservicenetty.config.NettyServerConfig;
import com.lwd.netservicenetty.core.ProtocolFrameDecoder;
import com.lwd.netservicenetty.core.ProtocolFrameEncoder;
import com.lwd.netservicenetty.core.internal.handler.ProtocolGuardHandler;
import com.lwd.netservicenetty.core.internal.handler.SafeTrafficHandler;
import com.lwd.netservicenetty.core.internal.handler.ServerTimeoutHandler;
import com.lwd.netservicenetty.protocol.ProtocolConstants;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Pipeline 装配工厂 — 入站/出站双重编解码
 *
 * @author Administrator
 */
@Component
@RequiredArgsConstructor
public class ChannelInitializer extends io.netty.channel.ChannelInitializer<SocketChannel> {

    private static final long WRITE_LIMIT_DISABLED = 0;
    private static final long READ_LIMIT_BPS = 2 * 1024 * 1024;
    private static final long CHECK_INTERVAL_MS = 1000;
    private static final int IDLE_TIMEOUT_SECONDS = 60;

    private final MonitorDataHandler monitorDataHandler;
    private final NettyServerConfig config;
    private final SafeTrafficHandler safeTrafficHandler;
    private final ServerTimeoutHandler serverTimeoutHandler;
    private final ProtocolGuardHandler protocolGuardHandler;
    private final ProtocolFrameDecoder protocolFrameDecoder;
    private final ProtocolFrameEncoder protocolFrameEncoder;

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // 出站：TcpPacket → ByteBuf 帧 → 网络
        pipeline.addLast(protocolFrameEncoder);

        // 入站：流量整形 → 日志 → 背压 → 心跳 → 超时 → 魔数校验 → 拆包 → 帧解码 → 业务
        pipeline.addLast(new ChannelTrafficShapingHandler(WRITE_LIMIT_DISABLED, READ_LIMIT_BPS, CHECK_INTERVAL_MS));
        pipeline.addLast(new LoggingHandler(config.logLevel()));
        pipeline.addLast(safeTrafficHandler);
        pipeline.addLast(new IdleStateHandler(IDLE_TIMEOUT_SECONDS, 0, 0, TimeUnit.SECONDS));
        pipeline.addLast(serverTimeoutHandler);
        pipeline.addLast(protocolGuardHandler);
        pipeline.addLast(new LengthFieldBasedFrameDecoder(
                ProtocolConstants.MAX_FRAME_LENGTH,
                ProtocolConstants.LENGTH_FIELD_OFFSET,
                ProtocolConstants.LENGTH_FIELD_LENGTH,
                ProtocolConstants.LENGTH_ADJUSTMENT, 0));
        pipeline.addLast(protocolFrameDecoder);
        pipeline.addLast(monitorDataHandler);
    }
}
