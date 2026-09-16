package com.tjxt.tjcommon.Autoconfigure.VirtualThread;

import com.tjxt.tjcommon.Utils.ThreadContextSnapshot;
import org.springframework.core.task.TaskDecorator;

/*
 * 线程上下文传递装饰器（Spring TaskDecorator）。
 * 职责：把「提交任务时的线程」的上下文（MDC + UserContext 等已注册 ThreadLocal）
 *      带到「执行任务的线程」上。
 * 使用：由 VirtualThreadConfig 装配到 @Async 的执行器上，业务侧无需感知。
 * 说明：
 *   - 虚拟线程不继承父线程 ThreadLocal，所以 @Async 方法里默认拿不到 requestId 和当前用户，
 *     必须靠本装饰器搬运
 *   - 之所以用自定义装饰器而不是 Boot 的 spring.task.execution.propagate-context：
 *     后者依赖 micrometer context-propagation（还需注册 Slf4jThreadLocalAccessor）才有 MDC 效果，
 *     本装饰器零额外依赖、行为确定
 * 注意：传递范围 = MDC + 通过 ThreadContextSnapshot.register(...) 注册过的 ThreadLocal。
 */
public class ThreadContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return ThreadContextSnapshot.wrap(runnable);
    }
}
