package com.lwd.netservicenetty.protocol.codec;

import com.lwd.netservicenetty.protocol.MessageCodec;
import com.lwd.netservicenetty.protocol.Heartbeat;
import io.netty.buffer.ByteBuf;
import org.springframework.stereotype.Component;

/**
 * 心跳编解码器
 *
 * @author Administrator
 */
@Component
public class HeartbeatCodec implements MessageCodec<Heartbeat> {

    @Override
    public int msgType() {
        return 0x03;
    }

    @Override
    public void encode(Heartbeat packet, ByteBuf out) {
        BcdUtils.writeBcd(out, packet.terminalId());
        out.writeShort(packet.seqNo());
        out.writeByte(packet.statusFlags());
    }

    @Override
    public Heartbeat decode(ByteBuf in) {
        String terminalId = BcdUtils.readBcd(in, 7); // 14 chars → 7 bytes BCD
        int seqNo = in.readUnsignedShort();
        byte statusFlags = in.readByte();
        return new Heartbeat(terminalId, seqNo, statusFlags);
    }
}
