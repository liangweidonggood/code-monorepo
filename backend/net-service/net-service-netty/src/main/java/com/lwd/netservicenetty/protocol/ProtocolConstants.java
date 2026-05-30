package com.lwd.netservicenetty.protocol;

/**
 * VTP 协议常量 — 集中管理所有魔法值
 *
 * @author Administrator
 */
public final class ProtocolConstants {

    private ProtocolConstants() { /* 常量类禁止实例化 */ }

    /** 帧头魔数 */
    public static final byte MAGIC_NUMBER = (byte) 0xFE;

    /** 最大帧长度 (字节) */
    public static final int MAX_FRAME_LENGTH = 1024;

    // 消息类型码 ──────────────────────────────

    /** 通用应答 (Client→Server) */
    public static final int MSG_COMMON_RESPONSE = 0x00;

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

    /** 数据透传 (双向) */
    public static final int MSG_DATA_TRANSMISSION = 0x12;

    /** 通用应答 (Server→Client) */
    public static final int MSG_COMMON_RESPONSE_SERVER = 0x80;

    /** 注册应答 */
    public static final int MSG_REGISTRATION_RESPONSE = 0x81;

    /** 立即回传指令 */
    public static final int MSG_IMMEDIATE_REPLAY = 0x82;

    /** 远程配置指令 */
    public static final int MSG_REMOTE_CONFIG = 0x83;

    // 子类型 ──────────────────────────────────

    /** 明文传输 */
    public static final byte SUB_TYPE_PLAIN = 0x00;

    /** AES 加密 */
    public static final byte SUB_TYPE_AES = 0x06;

    /** 国密加密 */
    public static final byte SUB_TYPE_SM4 = 0x07;

    // 帧结构偏移 ──────────────────────────────

    /** 帧头总字节数 (FE + len + type + sub) */
    public static final int HEADER_SIZE = 4;

    /** BCC 校验码字节数 */
    public static final int BCC_SIZE = 1;

    /** 长度字段偏移 (跳过魔数) */
    public static final int LENGTH_FIELD_OFFSET = 1;

    /** 长度字段字节数 */
    public static final int LENGTH_FIELD_LENGTH = 1;

    /** 帧尾修正值 (type + sub + BCC) */
    public static final int LENGTH_ADJUSTMENT = 3;
}
