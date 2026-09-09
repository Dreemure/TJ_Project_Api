package com.example.tj_project_apicommon.Autoconfigure.Mq;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.example.tj_project_apicommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * Spring Cloud Stream 消息发送助手。
 * 职责：封装 StreamBridge 发送消息，支持普通发送、延迟发送、异步发送（虚拟线程）。
 * 说明：需配合 RabbitMQ 延迟插件使用，且配置 spring.cloud.stream.rabbit.binder.delayed-exchange=true。
 */
@Slf4j
@Component
public class StreamMqHelper {

    private final StreamBridge streamBridge;
    private final Executor executor;

    public StreamMqHelper(StreamBridge streamBridge, Executor virtualThreadExecutor) {
        this.streamBridge = streamBridge;
        this.executor = virtualThreadExecutor;
    }

    /**
     * 发送消息（普通，无延迟）
     *
     * @param bindingName 绑定名称（对应 spring.cloud.stream.bindings.<bindingName>）
     * @param payload     消息体
     * @param <T>         消息类型
     */
    public <T> void send(String bindingName, T payload) {
        log.debug("准备发送消息，binding：{}， message：{}", bindingName, payload);
        Message<T> message = MessageBuilder.withPayload(payload)
                .setHeader(REQUEST_ID_HEADER, MDC.get(REQUEST_ID_HEADER))
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
                .setHeader(REQUEST_ID_HEADER, MDC.get(REQUEST_ID_HEADER))
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
        String requestId = MDC.get(REQUEST_ID_HEADER);
        CompletableFuture.runAsync(() -> {
            try {
                MDC.put(REQUEST_ID_HEADER, requestId);
                if (delayMillis != null && delayMillis > 0) {
                    sendDelayMessage(bindingName, payload, Duration.ofMillis(delayMillis));
                } else {
                    send(bindingName, payload);
                }
            } catch (Exception e) {
                log.error("异步发送消息异常，payload:{}", payload, e);
            }
        }, executor);
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