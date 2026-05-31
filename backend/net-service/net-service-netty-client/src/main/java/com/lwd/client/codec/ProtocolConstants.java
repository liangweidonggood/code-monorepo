package com.lwd.client.codec;

/**
 * VTP 协议常量 — 帧格式、消息类型码、子类型
 *
 * @author Administrator
 */
public final class ProtocolConstants {

    /** 经纬度编码缩放因子：10^6 */
    public static final int LAT_LNG_SCALE = 1_000_000;
    /** 鉴权码 BCD 字节长度：4 字节 = 8 个 hex 字符 */
    public static final int AUTH_CODE_BCD_LENGTH = 4;
    /** BCD 编码：每字节存 2 个十进制数字 */
    public static final int BCD_DIGITS_PER_BYTE = 2;

    /** 帧头魔数 */
    public static final byte MAGIC = (byte) 0xFE;
    /** 子类型：明文传输 */
    public static final byte SUB_PLAIN = 0x00;

    /** 注册请求 */
    public static final int MSG_REGISTRATION_REQUEST = 0x01;
    /** 鉴权请求 */
    public static final int MSG_AUTH_REQUEST = 0x02;
    /** 心跳 */
    public static final int MSG_HEARTBEAT = 0x03;
    /** 位置上报 */
    public static final int MSG_LOCATION_REPORT = 0x10;
    /** 告警上报 */
    public static final int MSG_ALARM_REPORT = 0x11;
    /** 通用应答（终端→服务端） */
    public static final int MSG_COMMON_RESPONSE = 0x00;
    /** 通用应答（服务端→终端） */
    public static final int MSG_COMMON_RESPONSE_SERVER = 0x80;
    /** 注册应答 */
    public static final int MSG_REGISTRATION_RESPONSE = 0x81;
    /** 立即回传指令（服务端→终端） */
    public static final int MSG_IMMEDIATE_REPLAY = 0x82;

    private ProtocolConstants() {
        throw new UnsupportedOperationException("常量类不可实例化");
    }
}
