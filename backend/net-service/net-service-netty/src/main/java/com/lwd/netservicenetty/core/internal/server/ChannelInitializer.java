package com.lwd.netservicenetty.core.internal.server;

import com.lwd.netservicenetty.business.MonitorDataHandler;
import com.lwd.netservicenetty.config.NettyServerConfig;
import com.lwd.netservicenetty.core.internal.codec.ProtocolFrameDecoder;
import com.lwd.netservicenetty.core.internal.codec.ProtocolFrameEncoder;
import com.lwd.netservicenetty.core.internal.handler.ProtocolGuardHandler;
import com.lwd.netservicenetty.core.internal.handler.SafeTrafficHandler;
import com.lwd.netservicenetty.core.internal.handler.ServerTimeoutHandler;
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
        pipeline.addLast(new ChannelTrafficShapingHandler(0, 2L * 1024 * 1024, 1000));
        pipeline.addLast(new LoggingHandler(config.logLevel()));
        pipeline.addLast(safeTrafficHandler);
        pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
        pipeline.addLast(serverTimeoutHandler);
        pipeline.addLast(protocolGuardHandler);
        pipeline.addLast(new LengthFieldBasedFrameDecoder(1024, 1, 1, 3, 0));
        pipeline.addLast(protocolFrameDecoder);
        pipeline.addLast(monitorDataHandler);
    }
}
