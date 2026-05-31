package com.lwd.netservicenetty.business;

import com.lwd.netservicenetty.protocol.ImmediateReplayCmd;
import com.lwd.netservicenetty.protocol.RemoteConfigCmd;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 指令下发 REST 接口 — 通过 HTTP 给指定在线终端下发指令
 *
 * @author Administrator
 */
@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class CommandController {

    /** 指令下发流水号起始值 */
    private static final int CMD_SEQ_IMMEDIATE_REPLAY = 200;
    /** 远程配置指令流水号起始值 */
    private static final int CMD_SEQ_REMOTE_CONFIG = 201;
    /** 响应 JSON 键常量 */
    private static final String KEY_SUCCESS = "success";

    private final DeviceRegistry registry;

    /** 在线终端列表 */
    @GetMapping
    public Map<String, Object> list() {
        List<String> ids = registry.listTerminalIds();
        return Map.of("count", ids.size(), "terminals", ids);
    }

    /** 下发立即回传指令 */
    @PostMapping("/{terminalId}/immediate-replay")
    public Map<String, Object> sendImmediateReplay(
            @PathVariable String terminalId,
            @RequestParam(defaultValue = "2") int intervalSeconds,
            @RequestParam(defaultValue = "30") int durationSeconds) {
        ChannelHandlerContext ctx = registry.lookup(terminalId);
        if (ctx == null || !ctx.channel().isActive()) {
            return Map.of(KEY_SUCCESS, false, "message", "终端不在线: " + terminalId);
        }
        ImmediateReplayCmd cmd = new ImmediateReplayCmd(terminalId, CMD_SEQ_IMMEDIATE_REPLAY, (byte) 0,
                intervalSeconds, durationSeconds);
        ctx.writeAndFlush(cmd).addListener(f -> {
            if (!f.isSuccess()) {
                log.error("立即回传发送失败", f.cause());
            }
        });
        log.info("【Web下发】立即回传 → 终端: {} 间隔: {}s 持续: {}s", terminalId, intervalSeconds, durationSeconds);
        return Map.of(KEY_SUCCESS, true, "terminalId", terminalId,
                "command", "immediate-replay",
                "interval", intervalSeconds, "duration", durationSeconds);
    }

    /** 下发远程配置指令 */
    @PostMapping("/{terminalId}/remote-config")
    public Map<String, Object> sendRemoteConfig(
            @PathVariable String terminalId,
            @RequestParam int paramId,
            @RequestParam String paramValue) {
        ChannelHandlerContext ctx = registry.lookup(terminalId);
        if (ctx == null || !ctx.channel().isActive()) {
            return Map.of(KEY_SUCCESS, false, "message", "终端不在线: " + terminalId);
        }
        RemoteConfigCmd cmd = new RemoteConfigCmd(terminalId, CMD_SEQ_REMOTE_CONFIG, paramId,
                paramValue.getBytes(StandardCharsets.US_ASCII));
        ctx.writeAndFlush(cmd).addListener(f -> {
            if (!f.isSuccess()) {
                log.error("远程配置发送失败", f.cause());
            }
        });
        log.info("【Web下发】远程配置 → 终端: {} 参数ID: {} 值: {}", terminalId, paramId, paramValue);
        return Map.of(KEY_SUCCESS, true, "terminalId", terminalId,
                "command", "remote-config", "paramId", paramId, "paramValue", paramValue);
    }
}
