package com.tjxt.tjcommon.Autoconfigure.VirtualThread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/*
 * 虚拟线程配置类（自动配置，见 META-INF/spring/...AutoConfiguration.imports）。
 * 职责：
 *   1. 注册虚拟线程执行器 Bean（名称固定 virtualThreadExecutor，供 StreamMqHelper 等按名称注入），
 *      并用 ThreadContextExecutor 包装，保证线程上下文（MDC/requestId + UserContext）跨虚拟线程传递
 *   2. 注册上下文传递装饰器 ThreadContextTaskDecorator —— @Async 的上下文传递靠它
 *   3. 兜底注册 @Async 用的 applicationTaskExecutor（仅当容器里没有同名 Bean 时）
 *
 * 【@Async 的执行器到底是谁提供的 —— 这里踩过坑，务必看清】
 *   自动配置的生效顺序是「Boot 自己的自动配置 → 第三方自动配置」，所以 Boot 的
 *   TaskExecutorConfigurations 会先于本类被处理。此时本类的 virtualThreadExecutor 还没注册，
 *   Boot 的 applicationTaskExecutor 判定条件（容器中没有 Executor Bean）成立，于是 Boot 正常创建它：
 *     - 各服务 application.yaml 里 spring.threads.virtual.enabled: true
 *       → Boot 创建的是虚拟线程版 SimpleAsyncTaskExecutor，@Async 本来就是虚拟线程
 *     - Boot 的构建器会收集容器里的 TaskDecorator Bean → 自动应用本类的 ThreadContextTaskDecorator
 *   结论：@Async 的执行器由 Boot 提供，本类只需要提供"装饰器"（数据），不要去抢这个名字。
 *
 *   ⚠ 如果本类也注册一个名叫 applicationTaskExecutor 的 Bean，就会与 Boot 的同名 Bean 冲突，
 *     启动直接抛 BeanDefinitionOverrideException（已由 tj-microservice-sdk 的用例实测暴露）。
 *     因此兜底 Bean 必须带 @ConditionalOnMissingBean(name = "applicationTaskExecutor")：
 *       - Boot 的 Bean 存在（正常情况）→ 本类退让，用 Boot 的执行器 + 本类的装饰器
 *       - Boot 的 Bean 不存在（例如某服务自己定义了 Executor Bean，把 Boot 的条件挡掉了）
 *         → 本类兜底提供「虚拟线程 + 上下文传递」的执行器，避免 @Async 退回 Spring 内部的
 *           平台线程 SimpleAsyncTaskExecutor（既非虚拟线程也不传递上下文）
 *
 * 使用：
 *   - 注入 Executor（@Qualifier("virtualThreadExecutor")）后 execute(...)，上下文自动传递
 *   - 或直接用 @Async（执行器已由 Boot 装配虚拟线程，并应用本类的上下文装饰器）
 *   - 自行 new 执行器 / CompletableFuture 未指定执行器时，用 ThreadContextSnapshot.wrap(task) 显式搬运
 * 注意：MDC 与 UserContext 底层都是普通 ThreadLocal，虚拟线程不继承父线程上下文，
 *      "传递"只能显式做，不会自动发生。
 */
@EnableAsync
@Configuration
@Slf4j
public class VirtualThreadConfig {

    /** @Async 执行器的 Bean 名（与 Boot 的约定一致） */
    private static final String APPLICATION_TASK_EXECUTOR = "applicationTaskExecutor";

    /** @Async 的兜底查找名（类型查找不唯一时 Spring 按这个名字找） */
    private static final String DEFAULT_TASK_EXECUTOR = "taskExecutor";

    /** 虚拟线程执行器（固定 Bean 名称，便于 @Qualifier 注入；已内建线程上下文传递） */
    @Bean
    public Executor virtualThreadExecutor() {
        log.info("已启用虚拟线程执行器 virtualThreadExecutor（含线程上下文传递）");
        return new ThreadContextExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * 线程上下文传递装饰器。
     * <p>Boot 的执行器构建器会自动收集容器中的 TaskDecorator Bean 并应用，
     * 所以这个 Bean 就是 @Async 能拿到 requestId / UserContext 的关键。
     */
    @Bean
    public TaskDecorator threadContextTaskDecorator() {
        return new ThreadContextTaskDecorator();
    }

    /**
     * @Async 执行器兜底：只有在容器里没有 applicationTaskExecutor 时才注册，避免与 Boot 的同名 Bean 冲突。
     * <p>正常情况（Boot 的自动配置先生效）下本 Bean 不会创建，@Async 用的是 Boot 的执行器 + 本类的装饰器。
     */
    @Bean(name = {APPLICATION_TASK_EXECUTOR, DEFAULT_TASK_EXECUTOR})
    @ConditionalOnMissingBean(name = APPLICATION_TASK_EXECUTOR)
    public AsyncTaskExecutor tjFallbackApplicationTaskExecutor(TaskDecorator threadContextTaskDecorator) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setThreadNamePrefix("tj-async-");
        executor.setVirtualThreads(true);                      // 每个任务一个虚拟线程
        executor.setTaskDecorator(threadContextTaskDecorator); // 搬运线程上下文
        log.info("未发现 applicationTaskExecutor，已由 tj-common 兜底提供（虚拟线程 + 线程上下文传递）");
        return executor;
    }
}
