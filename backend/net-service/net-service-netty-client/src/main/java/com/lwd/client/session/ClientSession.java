package com.lwd.client.session;

import com.lwd.client.config.ClientConfig;
import com.lwd.client.simulator.GpsSimulator;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 客户端会话共享状态 — 纯数据对象，提供线程安全的访问器。
 *
 * <p>字段使用 AtomicReference/AtomicInteger 保证跨 Netty 事件循环线程的可见性，
 * ScheduledFuture 泛型擦除使用 raw 类型。这是测试用客户端，不是生产级多线程应用。
 *
 * @author Administrator
 */
public class ClientSession {

    /** 客户端配置（不可变） */
    public final ClientConfig config;
    /** GPS 模拟器（不可变） */
    public final GpsSimulator simulator;

    /** 当前连接通道 */
    private final AtomicReference<Channel> curChannel = new AtomicReference<>();
    /** Netty 事件循环组 */
    private final AtomicReference<EventLoopGroup> eventGroup = new AtomicReference<>();
    /** 心跳定时任务 */
    private final AtomicReference<ScheduledFuture<?>> hbFuture = new AtomicReference<>();
    /** 位置上报定时任务 */
    private final AtomicReference<ScheduledFuture<?>> locFuture = new AtomicReference<>();
    /** 服务端返回的鉴权码 */
    private final AtomicReference<String> authStr = new AtomicReference<>();
    /** 消息流水号（线程安全自增） */
    private final AtomicInteger seqNo = new AtomicInteger();
    /** 重连尝试次数（线程安全） */
    private final AtomicInteger reconnectCount = new AtomicInteger();
    /** 断线回调 */
    private final AtomicReference<Runnable> discCallback = new AtomicReference<>();

    /**
     * 构造会话状态对象。
     *
     * @param config    客户端配置
     * @param simulator GPS 模拟器
     */
    public ClientSession(ClientConfig config, GpsSimulator simulator) {
        this.config = config;
        this.simulator = simulator;
    }

    /** 自增流水号 */
    public int nextSeq() {
        return seqNo.incrementAndGet();
    }

    /** 获取重连次数 */
    public int reconnectAttempt() {
        return reconnectCount.get();
    }

    /** 重连次数 +1 */
    public void incrementReconnectAttempt() {
        reconnectCount.incrementAndGet();
    }

    /** 重置重连次数 */
    public void resetReconnectAttempt() {
        reconnectCount.set(0);
    }

    /** 获取当前通道 */
    public Channel channel() {
        return curChannel.get();
    }

    /** 设置当前通道 */
    public void channel(Channel ch) {
        curChannel.set(ch);
    }

    /** 获取事件循环组 */
    public EventLoopGroup group() {
        return eventGroup.get();
    }

    /** 设置事件循环组 */
    public void group(EventLoopGroup grp) {
        eventGroup.set(grp);
    }

    /** 取消心跳定时任务 */
    public void cancelHeartbeatTask() {
        ScheduledFuture<?> task = hbFuture.getAndSet(null);
        if (task != null) {
            task.cancel(false);
        }
    }

    /** 设置心跳定时任务 */
    public void heartbeatTask(ScheduledFuture<?> task) {
        hbFuture.set(task);
    }

    /** 取消位置上报告时任务 */
    public void cancelLocationTask() {
        ScheduledFuture<?> task = locFuture.getAndSet(null);
        if (task != null) {
            task.cancel(false);
        }
    }

    /** 设置位置上报告时任务 */
    public void locationTask(ScheduledFuture<?> task) {
        locFuture.set(task);
    }

    /** 获取鉴权码 */
    public String authCode() {
        return authStr.get();
    }

    /** 设置鉴权码 */
    public void authCode(String code) {
        authStr.set(code);
    }

    /** 获取断线回调 */
    public Runnable onDisconnect() {
        return discCallback.get();
    }

    /** 设置断线回调 */
    public void onDisconnect(Runnable callback) {
        discCallback.set(callback);
    }
}
