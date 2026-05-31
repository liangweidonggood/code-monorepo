package com.lwd.netservicenetty.client.simulator;

import java.security.SecureRandom;

/**
 * GPS 轨迹模拟器 — 按速度和方向生成连续的模拟坐标
 *
 * @author Administrator
 */
public class GpsTrackSimulator {

    private static final int SPEED_MIN_KMH = 10;
    private static final int SPEED_MAX_KMH = 120;
    private static final double HEADING_DELTA_RAD = 0.5;
    private static final double SPEED_DELTA_KMH = 10;
    private static final int TURN_INTERVAL = 20;
    private static final double LAT_MIN = 18.0;
    private static final double LAT_MAX = 54.0;
    private static final double LNG_MIN = 73.0;
    private static final double LNG_MAX = 135.0;
    private static final int ALT_BASE_M = 30;
    private static final int ALT_RANGE_M = 20;
    private static final int SAT_MIN = 8;
    private static final int SAT_RANGE = 5;
    private static final int FUEL_DECREASE_RATE = 50;
    private static final SecureRandom RNG = new SecureRandom();

    private final double stepSize;
    private double lat;
    private double lng;
    private double speed;
    private double heading;
    private int counter;

    /**
     * @param startLat  起始纬度
     * @param startLng  起始经度
     * @param speedKmh  模拟速度 km/h
     * @param stepDeg   每次采样移动的度数步长
     */
    public GpsTrackSimulator(double startLat, double startLng, double speedKmh, double stepDeg) {
        this.lat = startLat;
        this.lng = startLng;
        this.speed = speedKmh;
        this.stepSize = stepDeg;
        this.heading = Math.toRadians(45);
    }

    /** 生成下一个 GPS 采样点 */
    public GpsPoint nextPosition() {
        counter++;
        if (counter % TURN_INTERVAL == 0) {
            heading += (RNG.nextDouble() - 0.5) * HEADING_DELTA_RAD;
            speed += (RNG.nextDouble() - 0.5) * SPEED_DELTA_KMH;
            if (speed < SPEED_MIN_KMH) {
                speed = SPEED_MIN_KMH;
            }
            if (speed > SPEED_MAX_KMH) {
                speed = SPEED_MAX_KMH;
            }
        }

        lat += Math.cos(heading) * stepSize;
        lng += Math.sin(heading) * stepSize;

        if (lat > LAT_MAX || lat < LAT_MIN) {
            heading = (heading + Math.PI) % (2 * Math.PI);
        }
        if (lng > LNG_MAX || lng < LNG_MIN) {
            heading = (heading + Math.PI) % (2 * Math.PI);
        }

        int alt = ALT_BASE_M + RNG.nextInt(ALT_RANGE_M);
        int sat = SAT_MIN + RNG.nextInt(SAT_RANGE);
        int fuel = Math.max(0, 100 - counter / FUEL_DECREASE_RATE);

        return new GpsPoint(lat, lng, (int) speed, alt, sat, fuel);
    }

    /** GPS 采样点 */
    public record GpsPoint(
            double latitude,
            double longitude,
            int speed,
            int altitude,
            int satellites,
            int fuelPercent
    ) {
    }
}
