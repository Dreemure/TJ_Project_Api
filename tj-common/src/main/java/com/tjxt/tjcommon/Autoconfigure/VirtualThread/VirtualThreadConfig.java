package com.tjxt.tjcommon.Autoconfigure.VirtualThread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/*
 * 虚拟线程配置类。
 * 职责：
 *   1. 注册虚拟线程执行器 Bean，名称固定为 virtualThreadExecutor（供 StreamMqHelper 等按名称注入）
 *   2. 开启 @Async 支持
 * 说明：
 *   - 虚拟线程由 Java 21+ 提供，不依赖池化，无需核心/最大线程数与队列容量
 *   - @Async 的执行器由 Spring Boot 提供（各服务 application.yaml 已配置 spring.threads.virtual.enabled: true，
 *     Boot 的 applicationTaskExecutor 本身就是虚拟线程执行器），因此这里不再实现 AsyncConfigurer，
 *     避免与业务自定义的 AsyncConfigurer 冲突（"Only one AsyncConfigurer may exist"）
 *   - 执行器 Bean 无条件注册：只要有服务需要按名称注入（如 MQ 异步发送），就不会出现缺 Bean
 * 使用：注入 Executor 即可（CompletableFuture.runAsync(task, executor)），或直接用 @Async。
 */
@EnableAsync
@Configuration
@Slf4j
public class VirtualThreadConfig {

    /** 虚拟线程执行器（固定 Bean 名称，便于 @Qualifier 注入） */
    @Bean
    public Executor virtualThreadExecutor() {
        log.info("已启用虚拟线程执行器 virtualThreadExecutor");
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
