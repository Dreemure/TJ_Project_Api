package com.tjxt.tjcommon.Autoconfigure.Mq;

import com.tjxt.tjcommon.Utils.MarkedRunnable;
import com.tjxt.tjcommon.Utils.RequestIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.tjxt.tjcommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * Spring Cloud Stream 消息发送助手。
 * 职责：封装 StreamBridge 发送消息，支持普通发送、延迟发送、异步发送（虚拟线程）。
 * 说明：
 *   - 需配合 RabbitMQ 延迟插件使用，且配置 spring.cloud.stream.rabbit.binder.delayed-exchange=true
 *   - 本类不作为 @Component（库内包不在业务服务扫描范围），由 MqConfig 以 @Bean 注册
 *   - 执行器按名称注入（virtualThreadExecutor，见 VirtualThreadConfig），避免多个 Executor Bean 歧义
 *   - 这里显式写 requestId 是「双保险」：即使 virtualThreadExecutor 被替换成裸执行器，
 *     消息头的链路ID也不会丢（全局拦截器 TraceIdChannelInterceptor 是最后一道兜底）
 */
@Slf4j
public class StreamMqHelper {

    private final StreamBridge streamBridge;
    private final Executor executor;

    public StreamMqHelper(StreamBridge streamBridge, @Qualifier("virtualThreadExecutor") Executor virtualThreadExecutor) {
        this.streamBridge = streamBridge;
        this.executor = virtualThreadExecutor;
    }

    /**
     * 发送消息（普通，无延迟）
     *
     * @param bindingName 绑定名称，就是交换机（对应 spring.cloud.stream.bindings.<bindingName>）
     * @param payload     消息体
     * @param <T>         消息类型
     */
    public <T> void send(String bindingName, T payload) {
        log.debug("准备发送消息，binding：{}， message：{}", bindingName, payload);
        Message<T> message = MessageBuilder.withPayload(payload)
                .setHeader(REQUEST_ID_HEADER, RequestIdUtil.getOrCreate())
                .build();
        streamBridge.send(bindingName, message);
    }

    /**
     * 发送延迟消息
     *
     * @param bindingName 绑定名称
     * @param payload     消息体
     * @param delay       延迟时长
     * @param <T>         消息类型
     */
    public <T> void sendDelayMessage(String bindingName, T payload, Duration delay) {
        log.debug("准备发送延迟消息，binding：{}， delay：{}ms， message：{}", bindingName, delay.toMillis(), payload);
        Message<T> message = MessageBuilder.withPayload(payload)
                .setHeader(REQUEST_ID_HEADER, RequestIdUtil.getOrCreate())
                .setHeader("x-delay", delay.toMillis())
                .build();
        streamBridge.send(bindingName, message);
    }

    /*
     * 异步发送消息（可带延迟），使用虚拟线程执行
     *
     * @param bindingName 绑定名称
     * @param payload     消息体
     * @param delayMillis 延迟毫秒数（null 或 <=0 表示无延迟）
     * @param <T>         消息类型
     */
    public <T> void sendAsync(String bindingName, T payload, Long delayMillis) {
        // 关键：MDC 是 ThreadLocal，虚拟线程不会继承父线程的上下文，
        // 所以必须在「提交任务的这一刻」用 MarkedRunnable 抓取快照，带到虚拟线程里恢复；
        // 原来手工 MDC.put(MDC.get(...)) 的写法在 MDC 为空时会把 null 也 put 进去，且依赖执行器实现。
        CompletableFuture.runAsync(MarkedRunnable.wrap(() -> {
            try {
                if (delayMillis != null && delayMillis > 0) {
                    sendDelayMessage(bindingName, payload, Duration.ofMillis(delayMillis));
                } else {
                    send(bindingName, payload);
                }
            } catch (Exception e) {
                log.error("异步发送消息异常，payload:{}", payload, e);
            }
        }), executor);
    }

    /*
     * 异步发送消息（无延迟），使用虚拟线程执行
     *
     * @param bindingName 绑定名称
     * @param payload     消息体
     * @param <T>         消息类型
     */
    public <T> void sendAsync(String bindingName, T payload) {
        sendAsync(bindingName, payload, null);
    }
}
