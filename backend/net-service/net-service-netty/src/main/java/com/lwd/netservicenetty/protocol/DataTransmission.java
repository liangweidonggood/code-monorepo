package com.lwd.netservicenetty.protocol;

import java.util.Arrays;

import org.jspecify.annotations.NonNull;


/**
 * 数据透传 — 双向自定义业务数据
 * <p>
 * Body: [终端号(14B BCD)] + [数据类型(2B)] + [数据长度(2B)] + [透传数据(N B)]
 *
 * @author Administrator
 */
public record DataTransmission(
        String terminalId,
        int dataType,
        byte[] payload
) implements TcpPacket {

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof DataTransmission(
                String tid, int dt, byte[] pld)
                && dataType == dt
                && terminalId.equals(tid)
                && Arrays.equals(payload, pld));
    }

    @Override
    public int hashCode() {
        int result = terminalId.hashCode();
        result = 31 * result + dataType;
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    @Override
    @NonNull
    public String toString() {
        return "DataTransmission[terminalId=" + terminalId
                + ", dataType=" + dataType
                + ", payload=" + Arrays.toString(payload) + "]";
    }
}
