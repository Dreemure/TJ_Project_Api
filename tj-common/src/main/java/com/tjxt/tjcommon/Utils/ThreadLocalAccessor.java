package com.tjxt.tjcommon.Utils;

import java.util.Objects;

/*
 * ThreadLocal 访问器：把某个 ThreadLocal 的「读取 / 恢复 / 清理」暴露给上下文快照机制。
 * 职责：让 ThreadContextSnapshot 能传递 MDC 之外的业务上下文（如 UserContext）。
 * 使用：
 *   - 业务侧注册：ThreadContextSnapshot.register(ThreadLocalAccessor.of(MY_THREAD_LOCAL));
 *   - 通常在 ThreadLocal 所属类的静态代码块里注册一次，避免遗忘
 * 说明：MDC 是 Map 而不是 ThreadLocal<T>，由 ThreadContextSnapshot 单独处理，不需要注册访问器。
 */
public interface ThreadLocalAccessor {

    /** 捕获当前线程的值（可能为 null，表示当前线程没有设置） */
    Object capture();

    /** 把捕获到的值恢复到当前线程；value 为 null 时应 remove 而不是 set(null) */
    void restore(Object value);

    /** 清理当前线程的值 */
    void clear();

    /**
     * 由一个 ThreadLocal 生成访问器。
     *
     * @param threadLocal 目标 ThreadLocal
     * @param <T>         值类型
     * @return 访问器
     */
    static <T> ThreadLocalAccessor of(ThreadLocal<T> threadLocal) {
        Objects.requireNonNull(threadLocal, "threadLocal must not be null");
        return new ThreadLocalAccessor() {
            @Override
            public Object capture() {
                return threadLocal.get();
            }

            @Override
            @SuppressWarnings("unchecked")
            public void restore(Object value) {
                if (value == null) {
                    threadLocal.remove();
                } else {
                    // 值来源就是同一个 ThreadLocal，强转安全
                    threadLocal.set((T) value);
                }
            }

            @Override
            public void clear() {
                threadLocal.remove();
            }
        };
    }
}
