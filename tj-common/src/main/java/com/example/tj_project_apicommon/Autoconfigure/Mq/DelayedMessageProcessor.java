package com.example.tj_project_apicommon.Autoconfigure.Mq;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;
import java.time.Duration;
import java.util.UUID;

import static com.example.tj_project_apicommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * 延迟消息处理器。
 * 职责：注入链路追踪ID(TraceId) + 设置RabbitMQ延迟头(x-delay)。
 * 使用：new DelayedMessageProcessor(duration).postProcessMessage(msg)。
 * 注意：需启用RabbitMQ延迟插件及spring.cloud.stream.rabbit.binder.delayed-exchange=true。
 */
public class DelayedMessageProcessor implements MessagePostProcessor {

    private final long delayMillis;

    public DelayedMessageProcessor(Duration delay) {
        this.delayMillis = delay.toMillis();
    }

    @Override
    public @NonNull Message<?> postProcessMessage(@NonNull Message<?> message) {
        String traceId = MDC.get(REQUEST_ID_HEADER);
        if (traceId == null) traceId = UUID.randomUUID().toString();

        return MessageBuilder.fromMessage(message)
                .setHeader(REQUEST_ID_HEADER, traceId)   // 链路追踪ID
                .setHeader("x-delay", delayMillis)       // RabbitMQ延迟插件专用头
                .build();
    }
}
