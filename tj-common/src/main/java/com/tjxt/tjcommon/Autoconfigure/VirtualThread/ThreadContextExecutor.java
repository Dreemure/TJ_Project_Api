package com.tjxt.tjcommon.Autoconfigure.VirtualThread;

import com.tjxt.tjcommon.Utils.ThreadContextSnapshot;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/*
 * 带线程上下文传递能力的 Executor 装饰器。
 * 职责：把「提交任务的线程」的上下文（MDC + UserContext 等已注册 ThreadLocal）搬运到
 *      「执行任务的线程」上，执行完自动清理。
 * 使用：new ThreadContextExecutor(Executors.newVirtualThreadPerTaskExecutor())，由 VirtualThreadConfig 注册为 Bean。
 * 说明：
 *   - 虚拟线程不继承父线程 ThreadLocal，裸的 newVirtualThreadPerTaskExecutor() 会丢掉
 *     requestId 和 UserContext，这里统一包一层，业务侧注入 Executor 后无需再关心
 *   - 只覆盖 execute()：submit / invokeAll / invokeAny 直接转发给底层执行器（未包装）。
 *     需要返回值时请用 CompletableFuture.runAsync/supplyAsync + ThreadContextSnapshot.wrap()，
 *     或改用 @Async（其执行器已由 ThreadContextTaskDecorator 处理）
 * 注意：实现 AutoCloseable，Spring 会在容器关闭时自动调用 close() 关闭底层执行器。
 */
public class ThreadContextExecutor implements Executor, AutoCloseable {

    private final ExecutorService delegate;

    public ThreadContextExecutor(ExecutorService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(ThreadContextSnapshot.wrap(command));
    }

    @Override
    public void close() {
        delegate.close();
    }
}
