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
 * Pipeline 装配工厂
 * <p>
 * 入站方向:
 *   流量整形(限速 2MB/s) → 日志 → 背压保护 → 空闲检测(60s) → 超时断开
 *   → 魔数校验(0xFE) → 长度拆包 → 协议解码(ByteBuf→TcpPacket) → 业务处理
 * <p>
 * 出站方向:
 *   协议编码(TcpPacket→ByteBuf) → 网络
 * <p>
 * 协议帧格式: [FE:1B] [len:1B] [type:1B] [sub:1B] [body:N B] [BCC:1B]
 * length 字段仅代表 body 的字节数，不含帧头帧尾。
 *
 * @author Administrator
 */
@Component
@RequiredArgsConstructor
public class ServerChannelInitializer extends io.netty.channel.ChannelInitializer<SocketChannel> {

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

        // 出站：TcpPacket → ByteBuf 帧
        pipeline.addLast(protocolFrameEncoder);

        // 入站：流量整形，限速 2MB/s 上传，每秒计算一次
        pipeline.addLast(new ChannelTrafficShapingHandler(0, 2L * 1024 * 1024, 1000));
        // 网络层日志
        pipeline.addLast(new LoggingHandler(config.logLevel()));
        // 背压保护：高水位停读，低水位恢复
        pipeline.addLast(safeTrafficHandler);
        // 心跳检测：60 秒无读触发 IdleStateEvent
        pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
        // 空闲超时断开（背压中的假空闲跳过）
        pipeline.addLast(serverTimeoutHandler);
        // 魔数校验：非 0xFE 直接掐断
        pipeline.addLast(protocolGuardHandler);
        // 帧拆包：按长度字段切出完整帧
        pipeline.addLast(new LengthFieldBasedFrameDecoder(
                ProtocolConstants.MAX_FRAME_LENGTH,
                ProtocolConstants.LENGTH_FIELD_OFFSET,
                ProtocolConstants.LENGTH_FIELD_LENGTH,
                ProtocolConstants.LENGTH_ADJUSTMENT,
                0));
        // 协议解码：ByteBuf 帧 → TcpPacket
        pipeline.addLast(protocolFrameDecoder);
        // 业务终点站
        pipeline.addLast(monitorDataHandler);
    }
}
