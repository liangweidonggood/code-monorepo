package com.lwd.netservicenetty.core.internal.codec;

import com.lwd.netservicenetty.protocol.TcpPacket;

import com.lwd.netservicenetty.protocol.MessageCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 消息编解码器注册表 — Spring 自动发现所有 MessageCodec Bean
 *
 * @author Administrator
 */
@Slf4j
@Component
public class MessageCodecRegistry {

    private final Map<Integer, MessageCodec<?>> codecMap;

    public MessageCodecRegistry(List<MessageCodec<?>> codecs) {
        this.codecMap = codecs.stream()
                .collect(Collectors.toUnmodifiableMap(
                        MessageCodec::msgType,
                        Function.identity(),
                        (existing, duplicate) -> {
                            throw new IllegalStateException(
                                    "重复的消息类型注册: " + existing.msgType());
                        }));
        log.info("已注册 {} 个消息编解码器: {}", codecMap.size(),
                codecMap.keySet().stream().map(k -> "0x" + Integer.toHexString(k).toUpperCase())
                        .sorted().collect(Collectors.joining(", ")));
    }

    @SuppressWarnings("unchecked")
    public <T extends com.lwd.netservicenetty.protocol.TcpPacket> MessageCodec<T> lookup(int msgType) {
        MessageCodec<?> codec = codecMap.get(msgType);
        if (codec == null) {
            throw new IllegalArgumentException(
                    "未注册的消息类型: 0x" + Integer.toHexString(msgType).toUpperCase());
        }
        return (MessageCodec<T>) codec;
    }
}
