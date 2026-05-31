package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.Heartbeat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HeartbeatCodec 单元测试。
 */
class HeartbeatCodecTest {

    private final HeartbeatCodec codec = new HeartbeatCodec();

    @Test
    void shouldEncodeHeartbeat() {
        Heartbeat packet = new Heartbeat("12345678901234", 42, (byte) 0x07);
        ByteBuf buf = Unpooled.buffer();
        codec.encode(packet, buf);
        assertThat(buf.readableBytes()).isEqualTo(10);
        buf.release();
    }

    @Test
    void shouldDecodeHeartbeatFields() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0x90, 0x12, 0x34});
        buf.writeShort(42);
        buf.writeByte(0x07);
        verifyDecodedHeartbeat(buf);
        buf.release();
    }

    private void verifyDecodedHeartbeat(ByteBuf buf) {
        Heartbeat packet = codec.decode(buf);
        assertThat(packet.terminalId()).isEqualTo("12345678901234");
        assertThat(packet.seqNo()).isEqualTo(42);
        assertThat(packet.statusFlags()).isEqualTo((byte) 0x07);
    }

    @Test
    void shouldRoundTrip() {
        Heartbeat original = new Heartbeat("98765432109876", 100, (byte) 0x03);
        ByteBuf buf = Unpooled.buffer();
        codec.encode(original, buf);
        Heartbeat decoded = codec.decode(buf);
        assertThat(decoded).isEqualTo(original);
        buf.release();
    }
}
