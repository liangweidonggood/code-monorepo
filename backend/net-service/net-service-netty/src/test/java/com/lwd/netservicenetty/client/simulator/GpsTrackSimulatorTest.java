package com.lwd.netservicenetty.client.simulator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * GPS 轨迹模拟器测试
 */
class GpsTrackSimulatorTest {

    @Test
    void shouldGenerateValidCoordinates() {
        var sim = new GpsTrackSimulator(39.9, 116.3, 80.0, 0.001);
        var point = sim.nextPosition();

        // 纬度范围: 18~54 (中国大陆)
        assertThat(point.latitude()).isBetween(18.0, 54.0);
        // 经度范围: 73~135
        assertThat(point.longitude()).isBetween(73.0, 135.0);
        // 速度 >= 0
        assertThat(point.speed()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldMoveInReasonableRange() {
        var sim = new GpsTrackSimulator(40.0, 116.0, 60.0, 0.0005);
        var p1 = sim.nextPosition();
        var p2 = sim.nextPosition();

        // 相邻点步长 0.0005°（约 55 米）
        double latDiff = Math.abs(p1.latitude() - p2.latitude());
        double lngDiff = Math.abs(p1.longitude() - p2.longitude());
        assertThat(latDiff).isLessThan(0.001);
        assertThat(lngDiff).isLessThan(0.001);
        // 至少移动了一些（步长 > 0）
        assertThat(latDiff + lngDiff).isGreaterThan(0);
    }

    @Test
    void shouldSimulateRealisticSpeed() {
        // 100km/h = 27.8 m/s，步长 0.0008° ≈ 89m（覆盖 1s 内的距离）
        var sim = new GpsTrackSimulator(39.9, 116.3, 100.0, 0.0008);
        var p1 = sim.nextPosition();
        var p2 = sim.nextPosition();

        double latDiff = Math.abs(p1.latitude() - p2.latitude());
        double lngDiff = Math.abs(p1.longitude() - p2.longitude());
        double distanceDeg = Math.sqrt(latDiff * latDiff + lngDiff * lngDiff);
        // 步长 0.0008° ± 20%
        assertThat(distanceDeg).isCloseTo(0.0008, within(0.0003));
    }
}
