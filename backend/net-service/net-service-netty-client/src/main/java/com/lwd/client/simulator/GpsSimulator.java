package com.lwd.client.simulator;

import com.lwd.client.model.GpsPoint;

/**
 * GPS 模拟器接口 — 依赖反转，方便测试替换。
 *
 * @author Administrator
 */
public interface GpsSimulator {

    /**
     * 生成下一个 GPS 采样点。
     *
     * @return GPS 采样点数据
     */
    GpsPoint nextPosition();
}
