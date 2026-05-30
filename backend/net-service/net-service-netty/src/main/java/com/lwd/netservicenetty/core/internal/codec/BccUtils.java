package com.lwd.netservicenetty.core.internal.codec;

import io.netty.buffer.ByteBuf;

/**
 * BCC 异或校验工具
 *
 * @author Administrator
 */
public final class BccUtils {

    private BccUtils() { /* 工具类禁止实例化 */ }

    /** 计算 BCC 异或校验码 */
    public static byte compute(ByteBuf body, int bodyLen) {
        int checksum = 0;
        for (int i = 0; i < bodyLen; i++) {
            checksum ^= body.getUnsignedByte(i);
        }
        return (byte) checksum;
    }

    /** 计算帧 BCC = XOR(length, msgType, subMsgType, body) */
    public static byte computeFrame(byte length, byte msgType, byte subMsgType, ByteBuf body) {
        int checksum = (length & 0xFF) ^ (msgType & 0xFF) ^ (subMsgType & 0xFF);
        int bodyLen = body.readableBytes();
        for (int i = 0; i < bodyLen; i++) {
            checksum ^= body.getUnsignedByte(i);
        }
        return (byte) checksum;
    }

    /** 从指定偏移开始异或 length 字节（解码器用） */
    public static byte computeFull(ByteBuf buf, int startIndex, int length) {
        int checksum = 0;
        for (int i = 0; i < length; i++) {
            checksum ^= buf.getUnsignedByte(startIndex + i);
        }
        return (byte) checksum;
    }
}
