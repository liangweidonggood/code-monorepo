package com.lwd.netservicenetty.protocol;

import java.util.Arrays;

import org.jspecify.annotations.NonNull;


/**
 * 远程配置指令 — 服务端下发参数修改
 *
 * <p>Body: [终端号(14B BCD)] + [流水号(2B)] + [参数ID(1B)] + [参数长度(1B)] + [参数值(N B)]
 *
 * @author Administrator
 */
public record RemoteConfigCmd(
        String terminalId,
        int seqNo,
        int paramId,
        byte[] paramValue
) implements TcpPacket {

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof RemoteConfigCmd(
                String tid, int ser, int pid, byte[] pval) &&
                seqNo == ser &&
                paramId == pid &&
                terminalId.equals(tid) &&
                Arrays.equals(paramValue, pval));
    }

    @Override
    public int hashCode() {
        int result = terminalId.hashCode();
        result = 31 * result + seqNo;
        result = 31 * result + paramId;
        result = 31 * result + Arrays.hashCode(paramValue);
        return result;
    }

    @Override
    @NonNull
    public String toString() {
        return "RemoteConfigCmd[terminalId=" + terminalId +
                ", seqNo=" + seqNo +
                ", paramId=" + paramId +
                ", paramValue=" + Arrays.toString(paramValue) + "]";
    }
}
