package com.lwd.client.model;

/**
 * 注册请求消息体 — 终端向服务端注册时发送。
 *
 * @author Administrator
 */
public record RegistrationBody(
        String terminalId,     // 终端号（14 位数字，BCD 编码）
        int manufacturerId,    // 厂商编号
        String terminalModel,  // 设备型号（4 位 hex 字符串，BCD 编码）
        int hwVer,             // 硬件版本
        int swVer,             // 软件版本
        int provinceId,        // 省域 ID
        int cityId,            // 市县域 ID
        String licensePlate,   // 车牌号（UTF-8 可变长度）
        int color              // 车牌颜色
) {
}
