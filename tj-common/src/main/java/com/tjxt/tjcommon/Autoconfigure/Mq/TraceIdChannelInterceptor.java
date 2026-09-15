package com.tjxt.tjcommon.Autoconfigure.Mq;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;

import static com.tjxt.tjcommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * 链路追踪ID拦截器（发送端）。
 * 职责：为出站消息注入 requestId（来源：MDC，取不到则生成 UUID）。
 * 说明：
 *   - 不作为 @Component（库内包不在业务服务的扫描范围），由 MqConfig 以 @Bean + @GlobalChannelInterceptor 注册
 *   - 消息头里已有 requestId 时直接返回（例如消费后再转发），保证不覆盖上一步的链路ID
 */
public class TraceIdChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        // 1. 已有链路ID：保持原样，避免覆盖上游传下来的值
        if (message.getHeaders().containsKey(REQUEST_ID_HEADER)) {
            return message;
        }
        // 2. 取 MDC 中的链路ID，没有则生成
        String traceId = MDC.get(REQUEST_ID_HEADER);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        // 3. Spring Messaging 的 Message 不可变，需要重新构建
        return MessageBuilder.fromMessage(message)
                .setHeader(REQUEST_ID_HEADER, traceId)
                .build();
    }
}
