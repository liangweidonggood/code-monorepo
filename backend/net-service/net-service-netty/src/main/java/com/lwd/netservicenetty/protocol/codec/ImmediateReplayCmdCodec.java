package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.ProtocolConstants;
import com.lwd.netservicenetty.protocol.ImmediateReplayCmd;
import io.netty.buffer.ByteBuf;
import org.springframework.stereotype.Component;

/**
 * 立即回传指令编解码器
 *
 * @author Administrator
 */
@Component
public class ImmediateReplayCmdCodec implements MessageCodec<ImmediateReplayCmd> {

    @Override
    public int msgType() {
        return ProtocolConstants.MSG_IMMEDIATE_REPLAY;
    }

    @Override
    public void encode(ImmediateReplayCmd p, ByteBuf out) {
        BcdUtils.writeBcd(out, p.terminalId());
        out.writeShort(p.seqNo());
        out.writeByte(p.flags());
        out.writeShort(p.uploadInterval());
        out.writeShort(p.duration());
    }

    @Override
    public ImmediateReplayCmd decode(ByteBuf in) {
        String terminalId = BcdUtils.readBcd(in, 7);
        int seqNo = in.readUnsignedShort();
        byte flags = in.readByte();
        int uploadInterval = in.readUnsignedShort();
        int duration = in.readUnsignedShort();
        return new ImmediateReplayCmd(terminalId, seqNo, flags, uploadInterval, duration);
    }
}
