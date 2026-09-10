package com.example.tj_project_apicommon.Autoconfigure.VirtualThread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/*
 * 虚拟线程配置类。
 * 职责：将虚拟线程执行器（Virtual Thread Executor）注册为 Spring Bean，供异步任务使用。
 * 说明：虚拟线程由 Java 21+ 提供，相比传统线程池，可大幅提升高并发场景下的吞吐量并降低资源开销。
 * 使用：注入 Executor 即可在 CompletableFuture.runAsync(Runnable, Executor) 中执行异步任务。
 * 注意：虚拟线程不依赖池化，无需配置核心/最大线程数及队列容量。
 */
@EnableAsync
@Configuration
@Slf4j
public class VirtualThreadConfig implements AsyncConfigurer {
    @Bean
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 显式指定 @Async 使用的执行器，避免依赖"唯一 Executor Bean"的隐式约定。
     */
    @Override
    public Executor getAsyncExecutor() {
        return virtualThreadExecutor();
    }

    /**
     * 异步任务异常处理器（可选，用于记录未捕获异常）。
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("异步任务执行异常，方法：{}", method.getName(), ex);
    }
}
