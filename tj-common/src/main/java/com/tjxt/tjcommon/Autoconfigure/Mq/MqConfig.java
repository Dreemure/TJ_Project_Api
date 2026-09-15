package com.tjxt.tjcommon.Autoconfigure.Mq;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.ChannelInterceptor;

import java.util.concurrent.Executor;

import static com.tjxt.tjcommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * Spring Cloud Stream 消息公共配置。
 * 职责：
 *   1. 注册消费端/发送端的 TraceId 拦截器（@GlobalChannelInterceptor 全局生效，自动应用到所有 channel）
 *   2. 注册消息发送助手 StreamMqHelper（依赖 StreamBridge + 虚拟线程执行器）
 * 说明：
 *   - 拦截器 Bean **必须**标注 @GlobalChannelInterceptor 才会被 Spring Integration 全局应用
 *     （原实现只在注释里写了"配合 GlobalChannelInterceptor"，注解缺失 → traceId 实际不会透传）
 *   - StreamMqHelper 用 @Qualifier("virtualThreadExecutor") 指定执行器，避免容器里存在
 *     applicationTaskExecutor 等多个 Executor Bean 时注入歧义
 *   - 未引入 spring-cloud-stream 的服务因 @ConditionalOnClass 自动跳过
 */
@Configuration
@ConditionalOnClass(MessageConverter.class)
public class MqConfig {

    /**
     * 消费端拦截器：从消息头中提取 TraceId 并放入 MDC（日志链路追踪）。
     * <p>与发送端 {@link TraceIdChannelInterceptor} 对称：一个"头 → MDC"，一个"MDC → 头"。
     */
    @Bean
    @GlobalChannelInterceptor(patterns = "*")
    public ChannelInterceptor receiveTraceIdInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                Object header = message.getHeaders().get(REQUEST_ID_HEADER);
                if (header != null) {
                    MDC.put(REQUEST_ID_HEADER, header.toString());
                }
                // 消息本身不变，仅做 MDC 设置
                return message;
            }
        };
    }

    /**
     * 发送端拦截器：为出站消息注入 TraceId（来源：MDC，取不到则生成 UUID）。
     */
    @Bean
    @GlobalChannelInterceptor(patterns = "*")
    @ConditionalOnMissingBean(TraceIdChannelInterceptor.class)
    public TraceIdChannelInterceptor traceIdChannelInterceptor() {
        return new TraceIdChannelInterceptor();
    }

    /**
     * 消息发送助手（普通/延迟/异步发送）。
     */
    @Bean
    @ConditionalOnClass(StreamBridge.class)
    @ConditionalOnMissingBean(StreamMqHelper.class)
    public StreamMqHelper streamMqHelper(StreamBridge streamBridge,
                                         @Qualifier("virtualThreadExecutor") Executor virtualThreadExecutor) {
        return new StreamMqHelper(streamBridge, virtualThreadExecutor);
    }
}
