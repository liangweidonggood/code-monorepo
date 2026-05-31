package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.LocationReport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LocationReportCodec 单元测试 — 核心业务消息。
 */
class LocationReportCodecTest {

    private final LocationReportCodec codec = new LocationReportCodec();

    @Test
    void shouldEncodeCorrectFrameLength() {
        LocationReport original = buildSampleReport();
        ByteBuf buf = Unpooled.buffer();
        codec.encode(original, buf);
        assertThat(buf.readableBytes()).isEqualTo(43);
        buf.release();
    }

    @Test
    void shouldRoundTrip() {
        LocationReport original = buildSampleReport();
        ByteBuf buf = Unpooled.buffer();
        codec.encode(original, buf);
        LocationReport decoded = codec.decode(buf);
        assertThat(decoded).isEqualTo(original);
        buf.release();
    }

    @Test
    void shouldDecodeAllFields() {
        LocationReport original = buildSampleReport();
        ByteBuf buf = Unpooled.buffer();
        codec.encode(original, buf);
        verifyDecodedFields(buf);
        buf.release();
    }

    private void verifyDecodedFields(ByteBuf buf) {
        LocationReport decoded = codec.decode(buf);
        assertThat(decoded.terminalId()).isEqualTo("12345678901234");
        assertThat(decoded.speed()).isEqualTo(80);
        assertThat(decoded.direction()).isEqualTo(180);
        assertThat(decoded.altitude()).isEqualTo(45);
        assertThat(decoded.mileage()).isEqualTo(123_456);
        assertThat(decoded.fuelPercent()).isEqualTo(85);
        assertThat(decoded.engineTemp()).isEqualTo(90);
        assertThat(decoded.batteryVoltage()).isEqualTo(1_250);
        assertThat(decoded.signalStrength()).isEqualTo(28);
        assertThat(decoded.satellites()).isEqualTo(10);
        assertThat(decoded.alarmFlags()).isEqualTo(0x0000_0001L);
        assertThat(decoded.gpsTime()).isEqualTo(LocalDateTime.of(2026, 5, 30, 14, 30, 0));
        assertThat(decoded.seqNo()).isEqualTo(1);
    }

    private LocationReport buildSampleReport() {
        return new LocationReport(
                "12345678901234",
                39.907_326,
                116.391_224,
                80,
                180,
                45,
                123_456,
                85,
                90,
                1_250,
                28,
                10,
                0x0000_0001L,
                LocalDateTime.of(2026, 5, 30, 14, 30, 0),
                1
        );
    }
}
