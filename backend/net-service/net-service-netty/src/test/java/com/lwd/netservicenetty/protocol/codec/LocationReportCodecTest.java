package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.LocationReport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * LocationReportCodec 单元测试 — 核心业务消息
 */
class LocationReportCodecTest {

    private final LocationReportCodec codec = new LocationReportCodec();

    @Test
    void shouldRoundTrip() {
        var original = new LocationReport(
                "12345678901234",      // terminalId (14 BCD)
                39.907326,              // latitude
                116.391224,             // longitude
                80,                     // speed km/h
                180,                    // direction
                45,                     // altitude
                123456,                 // mileage km
                85,                     // fuelPercent
                90,                     // engineTemp (90°C)
                1250,                   // batteryVoltage 12.50V
                28,                     // signalStrength
                10,                     // satellites
                0x00000001L,            // alarmFlags
                LocalDateTime.of(2026, 5, 30, 14, 30, 0), // gpsTime
                1                        // seqNo
        );

        ByteBuf buf = Unpooled.buffer();
        codec.encode(original, buf);

        // Body: 7 + 4+4 + 2+2+2+4 + 1+1+2+1+1+4 + 6 + 2 = 43 字节
        assertThat(buf.readableBytes()).isEqualTo(43);

        LocationReport decoded = codec.decode(buf);

        assertThat(decoded.terminalId()).isEqualTo("12345678901234");
        assertThat(decoded.latitude()).isCloseTo(39.907326, within(0.000001));
        assertThat(decoded.longitude()).isCloseTo(116.391224, within(0.000001));
        assertThat(decoded.speed()).isEqualTo(80);
        assertThat(decoded.direction()).isEqualTo(180);
        assertThat(decoded.altitude()).isEqualTo(45);
        assertThat(decoded.mileage()).isEqualTo(123456);
        assertThat(decoded.fuelPercent()).isEqualTo(85);
        assertThat(decoded.engineTemp()).isEqualTo(90);
        assertThat(decoded.batteryVoltage()).isEqualTo(1250);
        assertThat(decoded.signalStrength()).isEqualTo(28);
        assertThat(decoded.satellites()).isEqualTo(10);
        assertThat(decoded.alarmFlags()).isEqualTo(0x00000001L);
        assertThat(decoded.gpsTime()).isEqualTo(LocalDateTime.of(2026, 5, 30, 14, 30, 0));
        assertThat(decoded.seqNo()).isEqualTo(1);

        buf.release();
    }
}
