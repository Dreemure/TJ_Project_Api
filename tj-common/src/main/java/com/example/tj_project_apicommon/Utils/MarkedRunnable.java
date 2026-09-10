package com.example.tj_project_apicommon.Utils;

import org.slf4j.MDC;
import java.util.Map;
import java.util.Objects;

/*
 * MDC 上下文传递 Runnable 包装器。
 * 职责：捕获当前线程的 MDC 快照，在目标线程执行任务前恢复，执行后清理 MDC。
 * 使用：executor.execute(new MdcRunnable(task)) 或 MdcRunnable.wrap(task)。
 * 注意：仅传递 MDC，不传递其他 ThreadLocal；适用于线程池、虚拟线程等异步场景。
 */
public class MarkedRunnable implements Runnable{

    private final Runnable delegate;
    private final Map<String, String> context;

    public MarkedRunnable(Runnable delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.context = MDC.getCopyOfContextMap();
    }

    @Override
    public void run() {
        // 恢复父线程的 MDC 上下文；若为空则显式清空，避免复用线程残留数据
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
        try {
            delegate.run();
        } finally {
            MDC.clear(); // 清理，防止污染线程池中的其他任务
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
