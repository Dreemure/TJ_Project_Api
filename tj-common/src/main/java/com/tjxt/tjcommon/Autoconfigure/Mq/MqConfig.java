package com.tjxt.tjcommon.Autoconfigure.Mq;

import org.jspecify.annotations.NonNull; //非空注解，用来告诉编译器这个参数不应该为 null
import org.slf4j.MDC; // MDC 是线程本地的，不同线程互不共享
import org.springframework.beans.factory.annotation.Qualifier; // 当容器里有多个同类型 Bean 时，用 @Qualifier("beanName") 指定要注入哪一个
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass; // @ConditionalOnClass：classpath 里存在某个类时，这个配置/Bean 才生效
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean; // @ConditionalOnMissingBean：容器里没有某个类型的 Bean 时，才注册这个 Bean
import org.springframework.cloud.stream.function.StreamBridge; // Spring Cloud Stream 提供的编程式发送消息工具
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.GlobalChannelInterceptor; // 全局拦截器注解只有标注了它，ChannelInterceptor 才会被全局应用到所有 channel。patterns = "*" 表示匹配所有 channel 名
import org.springframework.messaging.Message; // Spring 消息抽象，包含 payload（消息体）和 headers（消息头）
import org.springframework.messaging.MessageChannel; // 消息通道，消息的发送/接收都经过它
import org.springframework.messaging.converter.MessageConverter; // 消息体与字节/JSON 之间的转换器。这里用它做 @ConditionalOnClass 的判断条件，确保 Spring Messaging 在 classpath 上
import org.springframework.messaging.support.ChannelInterceptor; // 通道拦截器，可以在消息发送前后做处理
import java.util.concurrent.Executor;

import static com.tjxt.tjcommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * Spring Cloud Stream 消息公共配置。
 * 职责：
 *   1. 注册消费端/发送端的 TraceId 拦截器（@GlobalChannelInterceptor 全局生效，自动应用到所有 channel）
 *   2. 注册消息发送助手 StreamMqHelper（依赖 StreamBridge + 虚拟线程执行器）
 * 说明：
 *   - 拦截器 Bean **必须**标注 @GlobalChannelInterceptor 才会被 Spring Integration 全局应用
 */
@Configuration
@ConditionalOnClass(MessageConverter.class)
public class MqConfig {

    /**
     * 消费端拦截器：从消息头中提取 TraceId 并放入 MDC，用于日志链路追踪。
     * <p>
     * MDC（Mapped Diagnostic Context）是日志框架提供的线程本地容器，底层为 ThreadLocal<Map<String, String>>;，
     * 它本身不是链路追踪，而是实现日志链路追踪的手段：把 traceId 放进当前线程的 MDC 后，
     * 日志配置中的 %X{requestId} 就会自动输出该 traceId，从而让同一线程内的所有日志都带上链路标识。
     * <p>
     * 本拦截器与发送端 {@link TraceIdChannelInterceptor} 对称：发送端负责“MDC → 消息头”，
     * 消费端负责“消息头 → MDC”。由于 MDC 是线程本地的，发送端和消费端通常不是同一个线程，
     * 因此必须在消费端重新 put 一次，消费线程的日志才能与发送端日志通过同一个 traceId 关联起来。
     * <p>
     * 注意：消费线程通常来自线程池，会被复用。务必在业务消费方法中通过 try/finally 清理 MDC
     * （例如 MDC.remove(REQUEST_ID_HEADER) 或 MDC.clear()），否则下一条消息复用该线程时，
     * 日志会残留上一条消息的 traceId，导致链路串号。
     */
    @Bean
    @GlobalChannelInterceptor(patterns = "*")
    public ChannelInterceptor receiveTraceIdInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                // 从消息头中取出 traceId
                // REQUEST_ID_HEADER 的字符串值（如 "requestId"）会作为 key 使用
                Object header = message.getHeaders().get(REQUEST_ID_HEADER);

                // 如果消息头里有 traceId，就放进当前消费线程的 MDC
                // MDC 是线程本地的，不是 JVM 全局的，也不是跨服务共享的。
                // 这里只是把消息头里的 traceId 复制到当前消费线程的 MDC 中
                if (header != null) MDC.put(REQUEST_ID_HEADER, header.toString());

                // 消息本身不变，仅做 MDC 设置
                return message;
            }
        };

        /*
         * 进入 preSend 前的 Message 示例：
         *
         * Message<Order> {
         *     headers: {
         *         "requestId": "abc-123",           // 发送端塞进来的 traceId
         *         "contentType": "application/json" // Spring Cloud Stream 自动加的内容类型
         *     },
         *     payload: Order {
         *         orderId: 1001,
         *         amount: 99.9
         *     }
         * }
         *
         * 构造这个 Message 的代码示例：
         *
         * Message<Order> message = MessageBuilder
         *         .withPayload(new Order(1001, 99.9))
         *         .setHeader("requestId", "abc-123")
         *         .setHeader("contentType", "application/json")
         *         .build();
         *
         * 执行 preSend 后，当前消费线程的 MDC：
         *
         * MDC = {
         *     "requestId" -> "abc-123"
         * }
         *
         * 业务消费方法里打印日志时，会自动带上 traceId：
         *
         * log.info("收到订单消息");
         * // 输出类似：14:30:05 [rabbit-ack-1] INFO [abc-123] - 收到订单消息
         *
         * 业务方法 finally 中清理 MDC：
         *
         * try {
         *     log.info("处理订单，requestId={}", MDC.get("requestId"));
         *     // 业务逻辑...
         * } finally {
         *     MDC.remove("requestId"); // 或 MDC.clear();
         * }
         */
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
