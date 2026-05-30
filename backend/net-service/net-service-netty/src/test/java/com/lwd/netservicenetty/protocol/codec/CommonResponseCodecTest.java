package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.CommonResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CommonResponseCodec 单元测试 — 编解码往返
 */
class CommonResponseCodecTest {

    private final CommonResponseCodec codec = new CommonResponseCodec();

    @Test
    void shouldEncodeCommonResponse() {
        var packet = new CommonResponse(1234, (short) 0x02, 0);
        ByteBuf buf = Unpooled.buffer();
        codec.encode(packet, buf);

        // Body: [serialNo(2B)] + [respMsgType(1B)] + [result(1B)] = 4 字节
        assertThat(buf.readableBytes()).isEqualTo(4);
        assertThat(buf.getUnsignedShort(0)).isEqualTo(1234);   // serialNo
        assertThat(buf.getUnsignedByte(2)).isEqualTo((short) 0x02);    // respMsgType
        assertThat(buf.getUnsignedByte(3)).isEqualTo((short) 0);       // result

        buf.release();
    }

    @Test
    void shouldDecodeCommonResponse() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(1234).writeByte(0x02).writeByte(0);

        CommonResponse packet = codec.decode(buf);

        assertThat(packet.serialNo()).isEqualTo(1234);
        assertThat(packet.respMsgType()).isEqualTo((short) 0x02);
        assertThat(packet.result()).isEqualTo((short) 0);

        buf.release();
    }

    @Test
    void shouldRoundTrip() {
        var original = new CommonResponse(5678, (short) 0x03, 1);
        ByteBuf buf = Unpooled.buffer();
        codec.encode(original, buf);
        CommonResponse decoded = codec.decode(buf);

        assertThat(decoded).isEqualTo(original);

        buf.release();
    }
}
