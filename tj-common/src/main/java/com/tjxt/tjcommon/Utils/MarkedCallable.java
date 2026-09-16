package com.tjxt.tjcommon.Utils;

import java.util.Objects;
import java.util.concurrent.Callable;

/*
 * 线程上下文传递 Callable 包装器（MarkedRunnable 的带返回值版本）。
 * 职责：捕获当前线程的「MDC + 已注册业务 ThreadLocal（如 UserContext）」快照，
 *      在目标线程执行前恢复，执行后清理。
 * 使用：CompletableFuture.supplyAsync(MarkedCallable.wrap(task), executor)
 * 说明：实际搬运逻辑在 ThreadContextSnapshot。
 * 注意：快照在「构造（wrap）时」捕获，应「先 wrap 再提交」，不要提前构造好长期持有。
 */
public class MarkedCallable<T> implements Callable<T> {

    private final Callable<T> delegate;
    private final ThreadContextSnapshot snapshot;

    public MarkedCallable(Callable<T> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.snapshot = ThreadContextSnapshot.capture();
    }

    @Override
    public T call() throws Exception {
        snapshot.restore();
        try {
            return delegate.call();
        } finally {
            snapshot.clear(); // 清理，防止污染线程池中的其他任务
        }
    }

    /**
     * 静态工厂方法，方便包装。
     *
     * @param callable 原始任务
     * @param <T>      返回值类型
     * @return 包装后的 MarkedCallable
     */
    public static <T> Callable<T> wrap(Callable<T> callable) {
        return new MarkedCallable<>(callable);
    }
}
