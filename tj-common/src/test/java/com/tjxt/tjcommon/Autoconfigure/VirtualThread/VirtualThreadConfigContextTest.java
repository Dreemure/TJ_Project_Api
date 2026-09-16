package com.tjxt.tjcommon.Autoconfigure.VirtualThread;

import com.tjxt.tjcommon.Utils.RequestIdUtil;
import com.tjxt.tjcommon.Utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * 容器级回归测试：本模块的自动配置必须能与 Boot 的 TaskExecutor 自动配置共存。
 *
 * 这里要钉住的是一段真实踩过的坑（已由 tj-microservice-sdk 的用例先暴露出来）：
 *   Boot 的 applicationTaskExecutor 与本模块的兜底 Bean 同名。如果本模块无条件注册这个名字，
 *   在「Boot 的自动配置先被处理」的真实启动顺序下就会抛 BeanDefinitionOverrideException，
 *   服务直接起不来。所以兜底 Bean 必须带 @ConditionalOnMissingBean(name = "applicationTaskExecutor")。
 *
 * 本用例刻意不假设"谁先处理"，只断言最终结果：
 *   1. 容器能正常启动（没有任何 Bean 定义冲突）—— 冲突回归点
 *   2. applicationTaskExecutor / taskExecutor 两个名字都能拿到同一个 Bean
 *   3. 不管执行器最终由 Boot 还是本模块提供，@Async 路径都必须是「虚拟线程 + 上下文传递」
 *      （Boot 提供时，靠的是它自动收集本模块的 TaskDecorator；本模块提供时则自己装配）
 */
class VirtualThreadConfigContextTest {

    private static final String REQUEST_ID = "trace-async-1";
    private static final Long USER_ID = 20001L;

    @AfterEach
    void tearDown() {
        MDC.clear();
        UserContext.removeUser();
    }

    @Test
    @DisplayName("与 Boot 的 TaskExecutor 自动配置同处一个容器：不冲突，且 @Async 是虚拟线程 + 传递上下文")
    void shouldCoexistWithBootAndPropagateContext() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        TaskExecutionAutoConfiguration.class,   // Boot 的（真实启动时通常先被处理）
                        VirtualThreadConfig.class))             // 本模块的
                .withPropertyValues("spring.threads.virtual.enabled=true")
                .run(context -> {
                    // 回归点：曾经在这里抛 BeanDefinitionOverrideException
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("virtualThreadExecutor");
                    assertThat(context).hasBean("applicationTaskExecutor");
                    assertThat(context).hasBean("taskExecutor");
                    assertThat(context).hasSingleBean(ThreadContextTaskDecorator.class);

                    // @Async 在类型查找不唯一时会按名字 taskExecutor 兜底，别名必须指向同一个 Bean
                    assertThat(context.getBean("taskExecutor"))
                            .isSameAs(context.getBean("applicationTaskExecutor"));

                    AsyncTaskExecutor executor =
                            context.getBean("applicationTaskExecutor", AsyncTaskExecutor.class);
                    assertThat(executor).isInstanceOf(SimpleAsyncTaskExecutor.class);

                    assertAsyncPathIsVirtualAndPropagatesContext(executor);
                });
    }

    @Test
    @DisplayName("Boot 的执行器不存在时由本模块兜底，且同样是虚拟线程 + 传递上下文")
    void shouldProvideFallbackWhenBootExecutorAbsent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VirtualThreadConfig.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("applicationTaskExecutor");

                    AsyncTaskExecutor executor =
                            context.getBean("applicationTaskExecutor", AsyncTaskExecutor.class);
                    assertThat(executor).isInstanceOf(SimpleAsyncTaskExecutor.class);

                    assertAsyncPathIsVirtualAndPropagatesContext(executor);
                });
    }

    /** 断言 @Async 路径的三个关键属性：跑在虚拟线程上、能读到 requestId、能读到 userId */
    private static void assertAsyncPathIsVirtualAndPropagatesContext(AsyncTaskExecutor executor) {
        RequestIdUtil.mark(REQUEST_ID);
        UserContext.setUser(USER_ID);

        CompletableFuture<Object[]> captured = new CompletableFuture<>();
        executor.execute(() -> captured.complete(new Object[]{
                RequestIdUtil.get(),
                UserContext.getUser(),
                Thread.currentThread().isVirtual()
        }));

        Object[] result;
        try {
            result = captured.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new AssertionError("@Async 执行任务失败", e);
        }
        assertThat(result[0]).as("requestId 必须传到 @Async 线程").isEqualTo(REQUEST_ID);
        assertThat(result[1]).as("UserContext 必须传到 @Async 线程").isEqualTo(USER_ID);
        assertThat(result[2]).as("@Async 必须使用虚拟线程").isEqualTo(Boolean.TRUE);
    }
}
