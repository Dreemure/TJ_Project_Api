package com.example.tj_project_apicommon.Autoconfigure.Mq;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import java.util.UUID;

import static com.example.tj_project_apicommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * 链路追踪ID拦截器（全局生效）。
 * 职责：为所有通过Spring Cloud Stream发送的消息自动注入TraceId（来源：MDC，若无则生成UUID）。
 * 使用：通过@Component自动注册为Spring Bean，无需手动调用。
 * 注意：依赖使用方@ComponentScan扫描此包；若作为Starter发布，建议改用@Configuration+@Bean方式注册。
 */
@Component
public class TraceIdChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        // 1. 尝试从 MDC 获取或生成 Trace ID
        String traceId = MDC.get(REQUEST_ID_HEADER);
        if (traceId == null) traceId = UUID.randomUUID().toString();

        // 2. 创建新的 Message 并添加 Header
        // 注意：Spring Messaging 的 Message 是不可变的，需要重新创建
        return org.springframework.messaging.support.MessageBuilder
                .fromMessage(message)
                .setHeader(REQUEST_ID_HEADER, traceId)
                .build();
    }
}
