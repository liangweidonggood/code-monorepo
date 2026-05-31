package com.lwd.netservicenetty.core.internal.codec;

import com.lwd.netservicenetty.core.ProtocolFrameDecoder;
import com.lwd.netservicenetty.core.ProtocolFrameEncoder;
import com.lwd.netservicenetty.protocol.CommonResponse;
import com.lwd.netservicenetty.protocol.codec.CommonResponseCodec;
import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 协议帧编解码器集成测试 — 编解码往返。
 */
class ProtocolCodecTest {

    private ProtocolFrameEncoder encoder;
    private ProtocolFrameDecoder decoder;

    @BeforeEach
    void setUp() {
        CommonResponseCodec codec = new CommonResponseCodec();
        MessageCodecRegistry registry = new MessageCodecRegistry(List.of(codec));
        encoder = new ProtocolFrameEncoder(registry);
        decoder = new ProtocolFrameDecoder(registry);
    }

    @Test
    void shouldEncodeFrameWithCorrectLength() {
        CommonResponse packet = new CommonResponse(1, (short) 0, 0);
        ByteBuf frame = encoder.encode(packet);
        assertThat(frame.readableBytes()).isEqualTo(9);
        frame.release();
    }

    @Test
    void shouldDecodeToCorrectType() {
        CommonResponse packet = new CommonResponse(1, (short) 0, 0);
        ByteBuf frame = encoder.encode(packet);
        Object result = decoder.decode(frame);
        assertThat(result).isInstanceOf(CommonResponse.class);
        frame.release();
    }

    @Test
    void shouldRoundTripFrame() {
        CommonResponse packet = new CommonResponse(1, (short) 0, 0);
        ByteBuf frame = encoder.encode(packet);
        verifyDecodedFrame(frame);
        frame.release();
    }

    private void verifyDecodedFrame(ByteBuf frame) {
        CommonResponse decoded = (CommonResponse) decoder.decode(frame);
        assertThat(decoded.serialNo()).isEqualTo(1);
        assertThat(decoded.respMsgType()).isEqualTo((short) 0);
        assertThat(decoded.result()).isZero();
    }
}
