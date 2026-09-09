package com.example.tj_project_apicommon.Autoconfigure.Mq;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.ChannelInterceptor;

import static com.example.tj_project_apicommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * Spring Cloud Stream 消息配置。
 * 职责：消费者端拦截器，从消息头提取 TraceId 并注入 MDC（日志链路追踪）。
 * 说明：Spring Cloud Stream 已自动支持 Jackson 序列化及消息 ID 生成，无需配置 MessageConverter。
 */
@Configuration
@ConditionalOnClass(MessageConverter.class)
public class MqConfig {

    /*
     * 消费者端拦截器：从消息头中提取 TraceId 并放入 MDC。
     * 与发送端的 TraceIdChannelInterceptor 对称，但职责不同。
     * 此处使用 @Bean 返回 ChannelInterceptor，并配合 GlobalChannelInterceptor 自动注册。
     */
    @Bean
    public ChannelInterceptor receiveTraceIdInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                // 从消息头获取 TraceId（由发送端注入）
                Object header = message.getHeaders().get(REQUEST_ID_HEADER);
                if (header != null) MDC.put(REQUEST_ID_HEADER, header.toString());
                // 消息不变，直接返回（仅做MDC设置）
                return message;
            }
        };
    }
}
