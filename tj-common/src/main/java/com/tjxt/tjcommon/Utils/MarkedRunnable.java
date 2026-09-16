package com.tjxt.tjcommon.Utils;

import java.util.Objects;

/*
 * 线程上下文传递 Runnable 包装器。
 * 职责：捕获当前线程的「MDC + 已注册业务 ThreadLocal（如 UserContext）」快照，
 *      在目标线程执行任务前恢复，执行后清理。
 * 使用：executor.execute(MarkedRunnable.wrap(task)) 或 ThreadContextSnapshot.wrap(task)。
 * 说明：实际搬运逻辑在 ThreadContextSnapshot，本类只是 Runnable 形态的壳，保留原类名以兼容既有调用。
 * 注意：
 *   - 底层 ThreadLocal 不是 InheritableThreadLocal，虚拟线程/线程池都不会自动继承，必须显式搬运
 *   - 快照在「构造（wrap）时」捕获，请先 wrap 再提交，不要提前构造后长期持有
 */
public class MarkedRunnable implements Runnable {

    private final Runnable delegate;
    private final ThreadContextSnapshot snapshot;

    public MarkedRunnable(Runnable delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.snapshot = ThreadContextSnapshot.capture();
    }

    @Override
    public void run() {
        snapshot.restore();
        try {
            delegate.run();
        } finally {
            snapshot.clear(); // 清理，防止污染线程池中的其他任务
        }
    }

    /**
     * 静态工厂方法，方便包装。
     *
     * @param runnable 原始任务
     * @return 包装后的 MarkedRunnable
     */
    public static Runnable wrap(Runnable runnable) {
        return new MarkedRunnable(runnable);
    }
}
