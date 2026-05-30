package com.lwd.netservicenetty.core.internal.codec;

import com.lwd.netservicenetty.core.ProtocolFrameDecoder;
import com.lwd.netservicenetty.core.ProtocolFrameEncoder;
import com.lwd.netservicenetty.core.internal.codec.MessageCodecRegistry;
import com.lwd.netservicenetty.protocol.CommonResponse;
import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.codec.CommonResponseCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 协议帧编解码器集成测试 — 编解码往返
 */
class ProtocolCodecTest {

    private ProtocolFrameEncoder encoder;
    private ProtocolFrameDecoder decoder;

    @BeforeEach
    void setUp() {
        var codec = new CommonResponseCodec();
        var registry = new MessageCodecRegistry(List.of(codec));
        encoder = new ProtocolFrameEncoder(registry);
        decoder = new ProtocolFrameDecoder(registry);
    }

    @Test
    void shouldEncodeAndDecodeFrame() {
        var packet = new CommonResponse(1, (short) 0, 0);

        ByteBuf frame = encoder.encode(packet);
        // 帧格式: FE len msgType 0x00 body... BCC
        // CommonResponse body: [00 01] [00] [00] = 4 bytes
        // len=4, msgType=0x00, subMsgType=0x00
        // BCC = 04 ^ 00 ^ 00 ^ 00 ^ 01 ^ 00 ^ 00 = 0x05
        assertThat(frame.readableBytes()).isEqualTo(9);

        Object result = decoder.decode(frame);
        assertThat(result).isInstanceOf(CommonResponse.class);
        var decoded = (CommonResponse) result;
        assertThat(decoded.serialNo()).isEqualTo(1);
        assertThat(decoded.respMsgType()).isEqualTo((short) 0);
        assertThat(decoded.result()).isEqualTo(0);

        frame.release();
    }
}
