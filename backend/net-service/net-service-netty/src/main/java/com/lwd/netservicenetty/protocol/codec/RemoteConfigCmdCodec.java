package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.RemoteConfigCmd;
import io.netty.buffer.ByteBuf;
import org.springframework.stereotype.Component;

/**
 * 远程配置指令编解码器
 *
 * @author Administrator
 */
@Component
public class RemoteConfigCmdCodec implements MessageCodec<RemoteConfigCmd> {

    @Override
    public int msgType() {
        return 0x83;
    }

    @Override
    public void encode(RemoteConfigCmd p, ByteBuf out) {
        BcdUtils.writeBcd(out, p.terminalId());
        out.writeShort(p.seqNo());
        out.writeByte(p.paramId());
        out.writeByte(p.paramValue().length);
        out.writeBytes(p.paramValue());
    }

    @Override
    public RemoteConfigCmd decode(ByteBuf in) {
        String terminalId = BcdUtils.readBcd(in, 7);
        int seqNo = in.readUnsignedShort();
        int paramId = in.readUnsignedByte();
        int valueLen = in.readUnsignedByte();
        byte[] paramValue = new byte[valueLen];
        in.readBytes(paramValue);
        return new RemoteConfigCmd(terminalId, seqNo, paramId, paramValue);
    }
}
