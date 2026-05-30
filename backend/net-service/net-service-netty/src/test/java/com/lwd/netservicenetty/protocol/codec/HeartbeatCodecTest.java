package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.Heartbeat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HeartbeatCodec 单元测试
 */
class HeartbeatCodecTest {

    private final HeartbeatCodec codec = new HeartbeatCodec();

    @Test
    void shouldEncodeHeartbeat() {
        var packet = new Heartbeat("12345678901234", 42, (byte) 0x07);
        ByteBuf buf = Unpooled.buffer();
        codec.encode(packet, buf);

        // Body: [terminalId:7B BCD] + [seqNo:2B] + [statusFlags:1B] = 10 字节
        assertThat(buf.readableBytes()).isEqualTo(10);

        buf.release();
    }

    @Test
    void shouldDecodeHeartbeat() {
        // 终端号 "12345678901234" = 14 BCD chars
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0x90, 0x12, 0x34}); // BCD terminalId
        buf.writeShort(42);
        buf.writeByte(0x07);

        Heartbeat packet = codec.decode(buf);

        assertThat(packet.terminalId()).isEqualTo("12345678901234");
        assertThat(packet.seqNo()).isEqualTo(42);
        assertThat(packet.statusFlags()).isEqualTo((byte) 0x07);

        buf.release();
    }

    @Test
    void shouldRoundTrip() {
        var original = new Heartbeat("98765432109876", 100, (byte) 0x03);
        ByteBuf buf = Unpooled.buffer();
        codec.encode(original, buf);
        Heartbeat decoded = codec.decode(buf);

        assertThat(decoded).isEqualTo(original);

        buf.release();
    }
}
