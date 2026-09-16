package com.tjxt.tjcommon.Autoconfigure.Mq;

import com.tjxt.tjcommon.Utils.RequestIdUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;

import static com.tjxt.tjcommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * 延迟消息处理器。
 * 职责：注入链路追踪ID(requestId) + 设置RabbitMQ延迟头(x-delay)。
 * 使用：new DelayedMessageProcessor(duration).postProcessMessage(msg)。
 * 说明：
 *   - 已有 requestId 时不覆盖，保证消费后再转发的场景链路不被打断（与 TraceIdChannelInterceptor 行为一致）
 *   - **只写消息头，不写 MDC**：本类由业务线程调用，往 MDC 写值会污染调用方后续日志
 * 注意：需启用RabbitMQ延迟插件及spring.cloud.stream.rabbit.binder.delayed-exchange=true。
 */
public class DelayedMessageProcessor implements MessagePostProcessor {

    private final long delayMillis;

    public DelayedMessageProcessor(Duration delay) {
        this.delayMillis = delay.toMillis();
    }

    @Override
    public @NonNull Message<?> postProcessMessage(@NonNull Message<?> message) {
        // 已有链路ID则保留（例如消费后再投递延迟消息），避免凭空开一条新链路
        Object existing = message.getHeaders().get(REQUEST_ID_HEADER);
        String traceId = (existing == null) ? RequestIdUtil.getOrCreate() : existing.toString();

        return MessageBuilder.fromMessage(message)
                .setHeader(REQUEST_ID_HEADER, traceId)   // 链路追踪ID
                .setHeader("x-delay", delayMillis)       // RabbitMQ延迟插件专用头
                .build();
    }
}
