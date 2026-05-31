package com.lwd.netservicenetty.protocol.codec;

import io.netty.buffer.ByteBuf;

/**
 * BCD 编解码工具 — 终端号等数字字段的压缩编码
 *
 * @author Administrator
 */
public final class BcdUtils {

    private BcdUtils() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /** 将 ByteBuf 中的 BCD 字节解码为数字字符串 */
    public static String readBcd(ByteBuf in, int length) {
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        StringBuilder sb = new StringBuilder(length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /** 将数字字符串编码为 BCD 写入 ByteBuf */
    public static void writeBcd(ByteBuf out, String digits) {
        if (digits.length() % 2 != 0) {
            throw new IllegalArgumentException("BCD 编码要求偶数位数字，实际: " + digits.length());
        }
        for (int i = 0; i < digits.length(); i += 2) {
            int high = Character.digit(digits.charAt(i), 16);
            int low = Character.digit(digits.charAt(i + 1), 16);
            out.writeByte((high << 4) | low);
        }
    }

    /** 编码经纬度：double 值 × 1,000,000 → int */
    public static int encodeLatLon(double value) {
        return (int) Math.round(value * 1_000_000);
    }

    /** 解码经纬度：int ÷ 1,000,000 → double */
    public static double decodeLatLon(int raw) {
        return raw / 1_000_000.0;
    }

    /** 编码 GPS 时间为 6 字节 BCD: YYMMDDHHmmss */
    public static void writeGpsTime(ByteBuf out, java.time.LocalDateTime time) {
        writeBcd(out, String.format("%02d%02d%02d%02d%02d%02d",
                time.getYear() % 100,
                time.getMonthValue(),
                time.getDayOfMonth(),
                time.getHour(),
                time.getMinute(),
                time.getSecond()));
    }

    /** 解码 6 字节 BCD 为 LocalDateTime */
    public static java.time.LocalDateTime readGpsTime(ByteBuf in) {
        String raw = readBcd(in, 6);
        return java.time.LocalDateTime.of(
                2000 + Integer.parseInt(raw.substring(0, 2)),
                Integer.parseInt(raw.substring(2, 4)),
                Integer.parseInt(raw.substring(4, 6)),
                Integer.parseInt(raw.substring(6, 8)),
                Integer.parseInt(raw.substring(8, 10)),
                Integer.parseInt(raw.substring(10, 12)));
    }
}
