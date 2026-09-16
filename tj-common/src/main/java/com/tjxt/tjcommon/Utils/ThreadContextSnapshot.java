package com.tjxt.tjcommon.Utils;

import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * 线程上下文快照 —— 跨线程（虚拟线程 / 线程池 / MQ 异步发送）传递上下文的统一机制。
 *
 * 职责：
 *   1. 捕获当前线程的 MDC + 所有已注册 ThreadLocal 的值
 *   2. 在目标线程恢复、执行完清理
 *   3. 提供 wrap(...) 静态工厂，业务侧一行就能带上下文投递任务
 *
 * 【为什么必须有这个机制】
 *   MDC 与 UserContext 底层都是普通 ThreadLocal（不是 InheritableThreadLocal），
 *   新建线程（包括虚拟线程）里一律是空的。虚拟线程不池化、不复用，
 *   所以问题不是"串号"，而是「每次异步跳转都丢上下文」：@Async 方法里
 *   requestId 为空、UserContext.getUser() 为 null，业务只能自己搬运 —— 这个类就是统一搬运点。
 *
 * 【扩展方式】
 *   其他业务 ThreadLocal（例如 SDK 的 UserContext）通过 register 注册一次访问器即可被自动搬运：
 *     static { ThreadContextSnapshot.register(ThreadLocalAccessor.of(HOLDER)); }
 *   注册是幂等的（同一个访问器实例只注册一次）。
 *
 * 使用：
 *   CompletableFuture.runAsync(ThreadContextSnapshot.wrap(task), executor);
 *   或在注入虚拟线程执行器（VirtualThreadConfig 提供的 virtualThreadExecutor）后直接 execute(...)
 * 注意：捕获发生在 capture()（即 wrap / 提交任务）那一刻，请「先 wrap 再提交」，不要长时间持有。
 */
public final class ThreadContextSnapshot {

    /** 已注册的业务 ThreadLocal 访问器（CopyOnWrite：注册很少、读取频繁且可能并发） */
    private static final CopyOnWriteArrayList<ThreadLocalAccessor> ACCESSORS = new CopyOnWriteArrayList<>();

    static {
        // tj-common 自带的用户上下文默认纳入传递范围（只传 userId，不含凭据）
        register(UserContext.ACCESSOR);
    }

    /** 一次捕获得到的「访问器 + 值」 */
    private record Captured(ThreadLocalAccessor accessor, Object value) {
    }

    private final Map<String, String> mdc;
    private final List<Captured> captured;

    private ThreadContextSnapshot(Map<String, String> mdc, List<Captured> captured) {
        this.mdc = mdc;
        this.captured = captured;
    }

    /**
     * 注册一个业务 ThreadLocal 访问器（幂等）。
     * <p>建议在 ThreadLocal 所属类的静态代码块中调用，避免业务侧忘记注册。
     *
     * @param accessor 访问器，通常由 {@link ThreadLocalAccessor#of(ThreadLocal)} 创建
     */
    public static void register(ThreadLocalAccessor accessor) {
        Objects.requireNonNull(accessor, "accessor must not be null");
        ACCESSORS.addIfAbsent(accessor);
    }

    /**
     * 捕获当前线程的上下文快照。
     *
     * @return 快照，可在任意线程 restore()
     */
    public static ThreadContextSnapshot capture() {
        List<ThreadLocalAccessor> accessors = ACCESSORS;
        List<Captured> captured = new ArrayList<>(accessors.size());
        for (ThreadLocalAccessor accessor : accessors) {
            captured.add(new Captured(accessor, accessor.capture()));
        }
        return new ThreadContextSnapshot(MDC.getCopyOfContextMap(), captured);
    }

    /** 把快照恢复到当前线程（覆盖当前值） */
    public void restore() {
        if (mdc == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(mdc);
        }
        for (Captured entry : captured) {
            entry.accessor().restore(entry.value());
        }
    }

    /** 清理当前线程中快照涉及的上下文（应在 finally 中调用，避免污染复用线程） */
    public void clear() {
        MDC.clear();
        for (Captured entry : captured) {
            entry.accessor().clear();
        }
    }

    /**
     * 包装 Runnable：在目标线程恢复提交时的上下文，执行完清理。
     *
     * @param runnable 原始任务
     * @return 带上下文的任务
     */
    public static Runnable wrap(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        ThreadContextSnapshot snapshot = capture();
        return () -> {
            snapshot.restore();
            try {
                runnable.run();
            } finally {
                snapshot.clear();
            }
        };
    }

    /**
     * 包装 Callable：在目标线程恢复提交时的上下文，执行完清理。
     *
     * @param callable 原始任务
     * @param <T>      返回值类型
     * @return 带上下文的任务
     */
    public static <T> Callable<T> wrap(Callable<T> callable) {
        Objects.requireNonNull(callable, "callable must not be null");
        ThreadContextSnapshot snapshot = capture();
        return () -> {
            snapshot.restore();
            try {
                return callable.call();
            } finally {
                snapshot.clear();
            }
        };
    }
}
