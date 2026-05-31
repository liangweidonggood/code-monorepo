package com.lwd.netservicenetty.business;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线终端注册表 — 内存维护 terminalId → ctx 映射
 *
 * @author Administrator
 */
@Slf4j
@Component
public class DeviceRegistry {

    private final Map<String, ChannelHandlerContext> devices = new ConcurrentHashMap<>();

    /** 注册在线终端 */
    public void register(String terminalId, ChannelHandlerContext ctx) {
        devices.put(terminalId, ctx);
        log.info("【设备上线】终端: {} (当前在线: {})", terminalId, devices.size());
    }

    /** 注销离线终端 */
    public void unregister(ChannelHandlerContext ctx) {
        devices.values().removeIf(v -> v.equals(ctx));
    }

    /** 根据终端号查找连接 */
    public ChannelHandlerContext lookup(String terminalId) {
        return devices.get(terminalId);
    }

    /** 获取所有在线终端号 */
    public List<String> listTerminalIds() {
        return List.copyOf(devices.keySet());
    }

    /** 在线终端数量 */
    public int onlineCount() {
        return devices.size();
    }
}
