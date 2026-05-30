package com.lwd.netservicenetty.client.simulator;

/**
 * GPS 轨迹模拟器 — 按速度和方向生成连续的模拟坐标
 *
 * @author Administrator
 */
public class GpsTrackSimulator {

    private final double stepSize;  // 每次移动的度数步长
    private double lat;
    private double lng;
    private double speed;
    private double heading;         // 方向角（弧度）
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
        this.heading = Math.toRadians(45); // 默认东北方向
    }

    /** 生成下一个 GPS 采样点 */
    public GpsPoint nextPosition() {
        counter++;
        // 每 20 个采样随机微调方向和速度 (模拟真实路况)
        if (counter % 20 == 0) {
            heading += (Math.random() - 0.5) * 0.5;     // ±0.25 rad 随机转弯
            speed += (Math.random() - 0.5) * 10;         // ±5 km/h 加减速
            if (speed < 10) speed = 10;
            if (speed > 120) speed = 120;
        }

        lat += Math.cos(heading) * stepSize;
        lng += Math.sin(heading) * stepSize;

        // 边界回弹
        if (lat > 54 || lat < 18) heading = (heading + Math.PI) % (2 * Math.PI);
        if (lng > 135 || lng < 73) heading = (heading + Math.PI) % (2 * Math.PI);

        int alt = 30 + (int) (Math.random() * 20);           // 30-50m 海拔
        int sat = 8 + (int) (Math.random() * 5);              // 8-12 卫星数
        int fuel = Math.max(0, 100 - counter / 50);           // 缓慢下降的油量

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
