package com.lwd.client.handler;

import com.lwd.client.session.ClientSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import static com.lwd.client.codec.FrameCodec.*;
import static com.lwd.client.codec.ProtocolConstants.*;

/**
 * 心跳发送器 — 定时发送心跳帧
 *
 * @author Administrator
 */
@ChannelHandler.Sharable
public class ClientHeartbeatHandler {

    /** 心跳状态位：ACC 开 */
    private static final byte HEARTBEAT_ACC_ON = (byte) 0x01;
    /** 共享会话状态 */
    private final ClientSession session;

    /**
     * 构造心跳发送器。
     *
     * @param session 共享会话状态
     */
    public ClientHeartbeatHandler(ClientSession session) {
        this.session = session;
    }

    /** 发送心跳帧（终端号 + 流水号 + 状态位） */
    public void send(ChannelHandlerContext ctx) {
        if (ctx.channel().isActive()) {
            ByteBuf body = encodeHeartbeat(session.config.terminalId(), session.nextSeq(), HEARTBEAT_ACC_ON);
            ClientProtocolHandler.writeFrame(ctx, MSG_HEARTBEAT, body);
        }
    }
}
