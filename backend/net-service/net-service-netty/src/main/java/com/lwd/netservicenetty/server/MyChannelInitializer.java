package com.lwd.netservicenetty.server;

import io.netty.channel.ChannelInitializer;
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
 * 假设数据包为
 *
 * @author Administrator
 */
@Component
@RequiredArgsConstructor
public class MyChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final MonitorDataHandler monitorDataHandler;
    private final NettyServerConfig config;
    private final SafeTrafficHandler safeTrafficHandler;
    private final ServerTimeoutHandler serverTimeoutHandler;
    private final ProtocolGuardHandler protocolGuardHandler;

    @Override
    protected void initChannel(SocketChannel ch) {
        // 1. 从通道中获取专属管道（Pipeline），开始组装流水线
        ChannelPipeline pipeline = ch.pipeline();
        // 挂载流量整形处理器（单连接限速）
        // writeLimit: 0 (不限制下载)
        // readLimit: 2MB/s (严格限制每个客户端上传流速最高 2MB/s)
        // checkInterval: 1000ms (每秒计算一次流速)
        pipeline.addLast(new ChannelTrafficShapingHandler(
                0,
                2L * 1024 * 1024,
                1000
        ));
        pipeline.addLast(new LoggingHandler(config.logLevel()));
        pipeline.addLast(safeTrafficHandler);
        // 心跳处理，60秒没数据过来就推送一个心跳事件
        pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
        pipeline.addLast(serverTimeoutHandler);
        pipeline.addLast(protocolGuardHandler);
        // 假设协议为fe 04 73 01 00 86 eb 02 19
        // 协议格式：[fe:魔数(1B)] + [04:长度(1B)] + [73:消息类型(1B)] + [01:子消息类型(1B)] + [动态消息体(N B)] + [19:CRC校验(1B)]
        // 核心逻辑：长度字段的值(04)仅代表[动态消息体]的长度。
        pipeline.addLast(new LengthFieldBasedFrameDecoder(
                1024,  // 最大帧上限 1KB，超过立刻触发防御机制抛异常
                1,     // 长度字段偏移 1 字节（精确跳过开头的魔数 fe）
                1,     // 长度字段本身占用 1 字节
                3,     // 灵魂修正值。实际长度值后[消息类型(1B) + 子消息类型(1B) + CRC(1B)]共 3 字节
                0      //  剥离字节数。设为 0 表示不剥离，将完整数据包原样传给业务 Handler 去剥洋葱解析
        ));
        pipeline.addLast(monitorDataHandler);
    }
}
