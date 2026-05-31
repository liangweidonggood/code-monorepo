package com.lwd.client.codec;

import com.lwd.client.model.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static com.lwd.client.codec.ProtocolConstants.*;

/**
 * 协议帧编解码工具 — BCD / BCC / 帧构建，自包含不依赖任何服务端代码
 *
 * @author Administrator
 */
public final class FrameCodec {

    private FrameCodec() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 构建完整帧：[FE] [bodyLen] [msgType] [subType] [body] [BCC]
     *
     * @param msgType 消息类型码
     * @param body    消息体字节数组
     * @return 完整帧的 ByteBuf
     */
    public static ByteBuf buildFrame(int msgType, byte[] body) {
        int frameLen = 1 + 1 + 1 + 1 + body.length + 1;
        ByteBuf buf = Unpooled.buffer(frameLen);
        byte bodyLen = (byte) body.length;
        buf.writeByte(MAGIC);
        buf.writeByte(bodyLen);
        buf.writeByte(msgType);
        buf.writeByte(SUB_PLAIN);
        buf.writeBytes(body);
        byte bcc = bodyLen;
        bcc ^= (byte) msgType;
        bcc ^= SUB_PLAIN;
        for (byte b : body) {
            bcc ^= b;
        }
        buf.writeByte(bcc);
        return buf;
    }

    /**
     * 解码帧头信息，返回消息类型码和 body slice（不含 BCC）
     *
     * @param frame 完整帧
     * @return 解码后的帧信息
     */
    public static DecodedFrame decodeFrame(ByteBuf frame) {
        frame.readByte(); // magic
        int bodyLen = frame.readUnsignedByte();
        int msgType = frame.readUnsignedByte();
        frame.readUnsignedByte(); // subType
        ByteBuf body = frame.readSlice(bodyLen);
        frame.readByte(); // bcc
        return new DecodedFrame(msgType, body);
    }

    /**
     * 将数字字符串按 BCD 编码写入 ByteBuf，要求偶数位数字
     *
     * @param out    目标 ByteBuf
     * @param digits 数字字符串（偶数位）
     */
    public static void writeBcd(ByteBuf out, String digits) {
        if (digits.length() % BCD_DIGITS_PER_BYTE != 0) {
            throw new IllegalArgumentException("BCD 编码要求偶数位数字，实际: " + digits.length());
        }
        for (int i = 0; i < digits.length(); i += BCD_DIGITS_PER_BYTE) {
            int high = Character.digit(digits.charAt(i), 16);
            int low = Character.digit(digits.charAt(i + 1), 16);
            out.writeByte((high << 4) | low);
        }
    }

    /**
     * 读取鉴权码（4 字节 BCD → 8 位 hex 字符串）
     *
     * @param in 源 ByteBuf
     * @return 鉴权码 hex 字符串
     */
    public static String readAuthCode(ByteBuf in) {
        byte[] bytes = new byte[AUTH_CODE_BCD_LENGTH];
        in.readBytes(bytes);
        StringBuilder sb = new StringBuilder(AUTH_CODE_BCD_LENGTH * BCD_DIGITS_PER_BYTE);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    /**
     * 经纬度 double → 整数编码（×1,000,000）
     *
     * @param value 经纬度 double 值
     * @return 编码后的 int
     */
    public static int encodeLatLon(double value) {
        return (int) Math.round(value * LAT_LNG_SCALE);
    }

    /**
     * GPS 时间 → 6 字节 BCD 写入（YYMMDDHHmmss）
     *
     * @param out  目标 ByteBuf
     * @param time GPS 时间
     */
    public static void writeGpsTime(ByteBuf out, LocalDateTime time) {
        writeBcd(out, String.format("%02d%02d%02d%02d%02d%02d",
                time.getYear() % 100, time.getMonthValue(), time.getDayOfMonth(),
                time.getHour(), time.getMinute(), time.getSecond()));
    }

    // ── 消息体编码 ──

    /**
     * 编码注册请求消息体
     *
     * @param req 注册请求消息体
     * @return 编码后的 ByteBuf
     */
    public static ByteBuf encodeRegistrationRequest(RegistrationBody req) {
        ByteBuf body = Unpooled.buffer();
        try {
            writeBcd(body, req.terminalId());
            body.writeShort(req.manufacturerId());
            writeBcd(body, req.terminalModel());
            body.writeByte(req.hwVer());
            body.writeByte(req.swVer());
            body.writeShort(req.provinceId());
            body.writeShort(req.cityId());
            byte[] plateBytes = req.licensePlate().getBytes(StandardCharsets.UTF_8);
            body.writeByte(plateBytes.length);
            body.writeBytes(plateBytes);
            body.writeByte(req.color());
            return body;
        } catch (Exception e) {
            body.release();
            throw e;
        }
    }

    /**
     * 编码鉴权请求消息体
     *
     * @param terminalId 终端号
     * @param authCode   鉴权码
     * @param seqNo      流水号
     * @return 编码后的 ByteBuf
     */
    public static ByteBuf encodeAuthRequest(String terminalId, String authCode, int seqNo) {
        ByteBuf body = Unpooled.buffer();
        try {
            writeBcd(body, terminalId);
            writeBcd(body, authCode);
            body.writeShort(seqNo);
            return body;
        } catch (Exception e) {
            body.release();
            throw e;
        }
    }

    /**
     * 编码心跳消息体
     *
     * @param terminalId  终端号
     * @param seqNo       流水号
     * @param statusFlags 状态位
     * @return 编码后的 ByteBuf
     */
    public static ByteBuf encodeHeartbeat(String terminalId, int seqNo, byte statusFlags) {
        ByteBuf body = Unpooled.buffer();
        try {
            writeBcd(body, terminalId);
            body.writeShort(seqNo);
            body.writeByte(statusFlags);
            return body;
        } catch (Exception e) {
            body.release();
            throw e;
        }
    }

    /**
     * 编码位置上报消息体
     *
     * @param loc 位置上报消息体
     * @return 编码后的 ByteBuf
     */
    public static ByteBuf encodeLocationReport(LocationBody loc) {
        ByteBuf body = Unpooled.buffer();
        try {
            writeBcd(body, loc.terminalId());
            body.writeInt(encodeLatLon(loc.lat()));
            body.writeInt(encodeLatLon(loc.lng()));
            body.writeShort(loc.speed());
            body.writeShort(loc.direction());
            body.writeShort(loc.altitude());
            body.writeInt(loc.mileage());
            body.writeByte(loc.fuelPercent());
            body.writeByte(loc.engineTemp());
            body.writeShort(loc.batteryVoltage());
            body.writeByte(loc.signalStrength());
            body.writeByte(loc.satellites());
            body.writeInt((int) loc.alarmFlags());
            writeGpsTime(body, loc.gpsTime());
            body.writeShort(loc.seqNo());
            return body;
        } catch (Exception e) {
            body.release();
            throw e;
        }
    }

    /**
     * 编码告警上报消息体
     *
     * @param alarm 告警上报消息体
     * @return 编码后的 ByteBuf
     */
    public static ByteBuf encodeAlarmReport(AlarmBody alarm) {
        ByteBuf body = Unpooled.buffer();
        try {
            writeBcd(body, alarm.terminalId());
            body.writeShort(alarm.alarmType());
            body.writeByte(alarm.alarmLevel());
            body.writeInt(encodeLatLon(alarm.lat()));
            body.writeInt(encodeLatLon(alarm.lng()));
            body.writeShort(alarm.speed());
            writeGpsTime(body, alarm.alarmTime());
            byte[] descBytes = alarm.description().getBytes(StandardCharsets.UTF_8);
            body.writeByte(descBytes.length);
            body.writeBytes(descBytes);
            return body;
        } catch (Exception e) {
            body.release();
            throw e;
        }
    }

    // ── 消息体解码 ──

    /**
     * 解码注册应答 Body: [流水号(2B)] [结果(1B)] [鉴权码(4B BCD)] [消息长度(1B)] [消息(NB)]
     *
     * @param in 源 ByteBuf
     * @return 注册应答
     */
    public static RegistrationResponse decodeRegistrationResponse(ByteBuf in) {
        int seqNo = in.readUnsignedShort();
        int result = in.readUnsignedByte();
        String authCode = readAuthCode(in);
        int msgLen = in.readUnsignedByte();
        byte[] msgBytes = new byte[msgLen];
        in.readBytes(msgBytes);
        String message = new String(msgBytes, StandardCharsets.UTF_8);
        return new RegistrationResponse(seqNo, result, authCode, message);
    }

    /**
     * 编码通用应答 Body: [流水号(2B)] [应答类型(1B)] [结果(1B)]
     *
     * @param resp 通用应答
     * @return 编码后的 ByteBuf
     */
    public static ByteBuf encodeCommonResponse(CommonResponse resp) {
        ByteBuf body = Unpooled.buffer(4);
        try {
            body.writeShort(resp.serialNo());
            body.writeByte(resp.respMsgType());
            body.writeByte(resp.result());
            return body;
        } catch (Exception e) {
            body.release();
            throw e;
        }
    }

    /**
     * 解码通用应答 Body: [流水号(2B)] [应答类型(1B)] [结果(1B)]
     *
     * @param in 源 ByteBuf
     * @return 通用应答
     */
    public static CommonResponse decodeCommonResponse(ByteBuf in) {
        int serialNo = in.readUnsignedShort();
        short respMsgType = (short) (in.readUnsignedByte() & 0xFF);
        int result = in.readUnsignedByte();
        return new CommonResponse(serialNo, respMsgType, result);
    }

    /**
     * 解码立即回传指令 Body: [终端号(7B BCD)] [流水号(2B)] [标志(1B)] [间隔(2B)] [持续时间(2B)]
     *
     * @param in 源 ByteBuf
     * @return 立即回传指令
     */
    public static ImmediateReplay decodeImmediateReplay(ByteBuf in) {
        in.skipBytes(7); // 跳过终端号 7 字节 BCD
        int seqNo = in.readUnsignedShort();
        byte flags = in.readByte();
        int uploadInterval = in.readUnsignedShort();
        int duration = in.readUnsignedShort();
        return new ImmediateReplay(seqNo, flags, uploadInterval, duration);
    }
}
