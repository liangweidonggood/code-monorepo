package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.CommonResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CommonResponseCodec 单元测试 — 编解码往返。
 */
class CommonResponseCodecTest {

    private final CommonResponseCodec codec = new CommonResponseCodec();

    @Test
    void shouldRoundTrip() {
        CommonResponse original = new CommonResponse(5678, (short) 0x03, 1);
        ByteBuf buf = Unpooled.buffer();
        codec.encode(original, buf);
        CommonResponse decoded = codec.decode(buf);
        assertThat(decoded).isEqualTo(original);
        buf.release();
    }

    @Test
    void shouldEncodeCorrectFrameLength() {
        CommonResponse packet = new CommonResponse(1234, (short) 0x02, 0);
        ByteBuf buf = Unpooled.buffer();
        codec.encode(packet, buf);
        assertThat(buf.readableBytes()).isEqualTo(4);
        buf.release();
    }

    @Test
    void shouldDecodeWithCorrectValues() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(1234).writeByte(0x02).writeByte(0);
        verifyDecodedFields(buf);
        buf.release();
    }

    private void verifyDecodedFields(ByteBuf buf) {
        CommonResponse packet = codec.decode(buf);
        assertThat(packet.serialNo()).isEqualTo(1234);
        assertThat(packet.respMsgType()).isEqualTo((short) 0x02);
        assertThat(packet.result()).isEqualTo((short) 0);
    }
}
