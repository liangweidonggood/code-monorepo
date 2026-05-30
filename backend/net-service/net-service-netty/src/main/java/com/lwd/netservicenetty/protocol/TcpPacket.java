package com.lwd.netservicenetty.protocol;

/**
 * TCP 协议消息统一类型入口 — 密封接口
 */
public sealed interface TcpPacket
        permits CommonResponse, Heartbeat, LocationReport,
                AuthRequest, RegistrationRequest, RegistrationResponse,
                AlarmReport, ImmediateReplayCmd, RemoteConfigCmd,
                DataTransmission {
}
