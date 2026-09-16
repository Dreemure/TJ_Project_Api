package com.tjxt.tjcommon.Autoconfigure.Mq;

import com.tjxt.tjcommon.Utils.RequestIdUtil;
import org.jspecify.annotations.NonNull; //非空注解，用来告诉编译器这个参数不应该为 null
import org.springframework.beans.factory.annotation.Qualifier; // 当容器里有多个同类型 Bean 时，用 @Qualifier("beanName") 指定要注入哪一个
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass; // @ConditionalOnClass：classpath 里存在某个类时，这个配置/Bean 才生效
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean; // @ConditionalOnMissingBean：容器里没有某个类型的 Bean 时，才注册这个 Bean
import org.springframework.cloud.stream.function.StreamBridge; // Spring Cloud Stream 提供的编程式发送消息工具
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.GlobalChannelInterceptor; // 全局拦截器注解：只有标注了它，ChannelInterceptor 才会被全局应用；patterns 匹配 channel 的 Bean 名
import org.springframework.messaging.Message; // Spring 消息抽象，包含 payload（消息体）和 headers（消息头）
import org.springframework.messaging.MessageChannel; // 消息通道，消息的发送/接收都经过它
import org.springframework.messaging.converter.MessageConverter; // 消息体与字节/JSON 之间的转换器。这里用它做 @ConditionalOnClass 的判断条件，确保 Spring Messaging 在 classpath 上
import org.springframework.messaging.support.ChannelInterceptor; // 通道拦截器，可以在消息发送前后做处理

import java.util.concurrent.Executor;

import static com.tjxt.tjcommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * Spring Cloud Stream 消息公共配置。
 * 职责：
 *   1. 注册消费端/发送端的 TraceId 拦截器（@GlobalChannelInterceptor 生效，按 patterns 应用到 channel）
 *   2. 注册消息发送助手 StreamMqHelper（依赖 StreamBridge + 虚拟线程执行器）
 * 说明：
 *   - 拦截器 Bean **必须**标注 @GlobalChannelInterceptor 才会被 Spring Integration 全局应用
 *   - 发送端只做「MDC → 消息头」，消费端只做「消息头 → MDC」；方向保持单向，互不越界
 */
@Configuration
@ConditionalOnClass(MessageConverter.class)
public class MqConfig {

    /*
     * 消费端拦截器：从消息头中提取 TraceId 并放入 MDC，用于日志链路追踪。
     *
     * MDC（Mapped Diagnostic Context）是日志框架提供的线程本地容器，底层为 ThreadLocal<Map<String, String>>，
     * 它本身不是链路追踪，而是实现日志链路追踪的手段：把 traceId 放进当前线程的 MDC 后，
     * 日志配置中的 %X{requestId} 就会自动输出该 traceId，从而让同一线程内的所有日志都带上链路标识。
     *
     * 本拦截器与发送端 TraceIdChannelInterceptor 对称：发送端负责「MDC → 消息头」，
     * 消费端负责「消息头 → MDC」。由于 MDC 是线程本地的，发送端和消费端通常不是同一个线程，
     * 因此必须在消费端重新 put 一次，消费线程的日志才能与发送端日志通过同一个 traceId 关联起来。
     *
     * 【为什么 patterns 是 "*-in-*" 而不是 "*"】
     *   patterns 匹配的是 channel 的 Bean 名。SCSt 的命名约定是
     *   「<functionName>-in-<index>」为输入通道、「<functionName>-out-<index>」为输出通道，
     *   本项目的绑定名同样遵循该约定（见 MqConstants.Binding 注释）。
     *   本拦截器是「往当前线程 MDC 里写值」的：如果它也作用在发送端通道上，
     *   就会在业务线程（Tomcat / 定时任务）里凭空塞进一个 requestId —— 那不是这次请求的 id，
     *   却会挂到该线程后续的所有日志上，比留空更糟。限定到输入通道即可避免。
     *   风险提示：以后若自定义了不遵循 -in-/-out 命名的通道，需同步调整这里的 patterns，
     *   否则消费端不会写入 MDC（表现为消费日志没有 requestId）。
     *
     * 【为什么消息头没有 requestId 也要生成一个】
     *   消费线程通常来自线程池、会被复用。若采用「没有就不覆盖」的策略，
     *   消费线程会沿用上一条消息残留的 requestId（串号），比没有 id 更危险。
     *   所以这里的策略是「一定设置」：有就用上游的，没有就生成。
     *
     * 注意：业务消费方法仍建议在 finally 中清理（RequestIdUtil.clear()），
     *      避免消费线程被复用、或在同一线程里转发消息时沿用这次的 id。
     */
    @Bean
    @GlobalChannelInterceptor(patterns = "*-in-*")
    public ChannelInterceptor receiveTraceIdInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                // 从消息头中取出 requestId（REQUEST_ID_HEADER 的字符串值 "requestId" 作为 key）
                Object header = message.getHeaders().get(REQUEST_ID_HEADER);
                String requestId = (header == null) ? null : header.toString();
                // 没有则生成：本消费线程的 MDC 一定要有值，且不能被上一条消息的残留值污染
                if (requestId == null || requestId.isBlank()) {
                    requestId = RequestIdUtil.generate();
                }
                RequestIdUtil.mark(requestId);
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
         *     payload: Order { orderId: 1001, amount: 99.9 }
         * }
         *
         * 执行 preSend 后，当前消费线程的 MDC：
         *
         * MDC = { "requestId" -> "abc-123" }
         *
         * 业务消费方法里打印日志时，会自动带上 traceId：
         *
         * log.info("收到订单消息");
         * // 输出类似：14:30:05 [rabbit-ack-1] INFO [abc-123] - 收到订单消息
         *
         * 业务方法 finally 中清理：
         *
         * try {
         *     log.info("处理订单，requestId={}", RequestIdUtil.get());
         *     // 业务逻辑...
         * } finally {
         *     RequestIdUtil.clear();
         * }
         */
    }

    /**
     * 发送端拦截器：为出站消息注入 requestId（来源：MDC，取不到则生成）。
     * <p>只写消息头、不写 MDC：它跑在调用方线程（Tomcat / 定时任务 / 消费线程）上，
     * 往这些线程的 MDC 里塞值会污染调用方后续日志，而拦截器本身没有清理时机。
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
