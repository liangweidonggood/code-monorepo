package com.lwd.client.handler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * 借用 Netty 共享 Executor — 实现 {@link AutoCloseable} 但 close 为空操作。
 *
 * <p>Java 19+ 中 {@link ScheduledExecutorService} 继承 {@link AutoCloseable}，
 * 导致静态分析工具对 {@code ch.eventLoop()} 回报"资源未关闭"。
 * 实际 Netty EventLoop 生命周期由 Channel 管理，此处仅为借用，不可关闭。
 *
 * <p>方法返回 void 避免暴露 {@link ScheduledFuture}{@code <?>} wildcard，
 * 通过 {@link Consumer} 回调注册任务句柄以支持后续取消。
 *
 * @author Administrator
 */
public final class BorrowedExecutor implements AutoCloseable {

    private final ScheduledExecutorService delegate;

    private BorrowedExecutor(ScheduledExecutorService delegate) {
        this.delegate = delegate;
    }

    /** 构造借用器，包装 Netty 共享 EventLoop */
    public static BorrowedExecutor of(ScheduledExecutorService executor) {
        return new BorrowedExecutor(executor);
    }

    /**
     * 按固定频率调度任务，通过 {@code registrar} 注册句柄以支持取消。
     *
     * @param command      任务
     * @param initialDelaySeconds 首次延迟（秒）
     * @param periodSeconds       执行间隔（秒）
     * @param registrar           接收 ScheduledFuture 的注册回调（通常为 {@code session::heartbeatTask}）
     */
    public void scheduleAtFixedRate(Runnable command,
                                            long initialDelaySeconds,
                                            long periodSeconds,
                                            Consumer<ScheduledFuture<?>> registrar) {
        registrar.accept(
                delegate.scheduleAtFixedRate(command, initialDelaySeconds, periodSeconds, SECONDS));
    }

    /**
     * 一次性延时任务 — 不返回句柄，调用方无需取消。
     *
     * @param command 任务
     * @param delaySeconds 延迟（秒）
     */
    public void schedule(Runnable command, long delaySeconds) {
        delegate.schedule(command, delaySeconds, SECONDS);
    }

    /** 不关闭 — 资源生命周期由 Netty Channel 管理 */
    @Override
    public void close() {
        // Netty EventLoop 由 Channel 管理，此处不关闭
    }
}
