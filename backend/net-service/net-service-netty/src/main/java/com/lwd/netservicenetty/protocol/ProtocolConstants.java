package com.lwd.netservicenetty.protocol;

/**
 * VTP 协议常量 — 仅协议规范级别的共享常量
 *
 * @author Administrator
 */
public final class ProtocolConstants {

    private ProtocolConstants() {
        throw new UnsupportedOperationException("常量类不可实例化");
    }

    /** 帧头魔数 */
    public static final byte MAGIC_NUMBER = (byte) 0xFE;

    /** 最大帧长度 */
    public static final int MAX_FRAME_LENGTH = 1024;

    // ── 消息类型码 ──

    public static final int MSG_COMMON_RESPONSE = 0x00;
    public static final int MSG_REGISTRATION_REQUEST = 0x01;
    public static final int MSG_AUTH_REQUEST = 0x02;
    public static final int MSG_HEARTBEAT = 0x03;
    public static final int MSG_LOCATION_REPORT = 0x10;
    public static final int MSG_ALARM_REPORT = 0x11;
    public static final int MSG_DATA_TRANSMISSION = 0x12;
    public static final int MSG_COMMON_RESPONSE_SERVER = 0x80;
    public static final int MSG_REGISTRATION_RESPONSE = 0x81;
    public static final int MSG_IMMEDIATE_REPLAY = 0x82;
    public static final int MSG_REMOTE_CONFIG = 0x83;

    // ── 子类型 ──

    public static final byte SUB_TYPE_PLAIN = 0x00;
    public static final byte SUB_TYPE_AES = 0x06;
    public static final byte SUB_TYPE_SM4 = 0x07;

    // ── 帧结构偏移 ──

    /** 长度字段偏移（跳过魔数） */
    public static final int LENGTH_FIELD_OFFSET = 1;
    /** 长度字段占用字节数 */
    public static final int LENGTH_FIELD_LENGTH = 1;
    /** 帧尾修正值: type(1) + sub(1) + BCC(1) = 3 */
    public static final int LENGTH_ADJUSTMENT = 3;
}
