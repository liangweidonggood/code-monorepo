package com.lwd.client.simulator;

import com.lwd.client.model.GpsPoint;

import java.security.SecureRandom;

/**
 * GPS 轨迹模拟器 — 按速度和方向生成连续的模拟坐标，模拟车辆在城市道路上的移动。
 *
 * @author Administrator
 */
public class GpsTrackSimulator implements GpsSimulator {

    /** 模拟速度下限 km/h */
    private static final int SPEED_MIN_KMH = 10;
    /** 模拟速度上限 km/h */
    private static final int SPEED_MAX_KMH = 120;
    /** 航向随机变化幅度 rad */
    private static final double HEADING_DELTA_RAD = 0.5;
    /** 速度随机变化幅度 km/h */
    private static final double SPEED_DELTA_KMH = 10;
    /** 每 N 次采样后随机改变航向和速度 */
    private static final int TURN_INTERVAL = 20;
    /** 模拟纬度下限（中国南海） */
    private static final double LAT_MIN = 18.0;
    /** 模拟纬度上限（中国漠河） */
    private static final double LAT_MAX = 54.0;
    /** 模拟经度下限（中国西部） */
    private static final double LNG_MIN = 73.0;
    /** 模拟经度上限（中国东部） */
    private static final double LNG_MAX = 135.0;
    /** 海拔基准值 m */
    private static final int ALT_BASE_M = 30;
    /** 海拔随机浮动范围 m */
    private static final int ALT_RANGE_M = 20;
    /** 最小卫星数 */
    private static final int SAT_MIN = 8;
    /** 卫星数随机浮动范围 */
    private static final int SAT_RANGE = 5;
    /** 每 N 次采样燃油下降 1% */
    private static final int FUEL_DECREASE_RATE = 50;
    /** 安全随机数生成器 */
    private static final SecureRandom RNG = new SecureRandom();

    /** 每次采样移动的度数步长 */
    private final double stepSize;
    /** 当前纬度 */
    private double lat;
    /** 当前经度 */
    private double lng;
    /** 当前速度 km/h */
    private double speed;
    /** 当前航向弧度 */
    private double heading;
    /** 采样计数器 */
    private int counter;

    /**
     * 创建 GPS 轨迹模拟器。
     *
     * @param startLat 起始纬度
     * @param startLng 起始经度
     * @param speedKmh 模拟速度 km/h
     * @param stepDeg  每次采样移动的度数步长
     */
    public GpsTrackSimulator(double startLat, double startLng, double speedKmh, double stepDeg) {
        this.lat = startLat;
        this.lng = startLng;
        this.speed = speedKmh;
        this.stepSize = stepDeg;
        this.heading = Math.toRadians(45);
    }

    /**
     * 生成下一个 GPS 采样点，模拟车辆移动。
     *
     * @return GPS 采样点
     */
    @Override
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
}
