package com.tjxt.tjcommon.Autoconfigure.Mq;

import com.tjxt.tjcommon.Utils.RequestIdUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;

import static com.tjxt.tjcommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * 链路追踪ID拦截器（发送端）。
 * 职责：为出站消息注入 requestId（来源：MDC，取不到则生成）。
 * 说明：
 *   - 不作为 @Component（库内包不在业务服务的扫描范围），由 MqConfig 以 @Bean + @GlobalChannelInterceptor 注册
 *   - 消息头里已有 requestId 时直接返回（例如消费后再转发），保证不覆盖上一步的链路ID
 *   - **只写消息头，不写 MDC（即「导出」inject）**：本拦截器是全局的（patterns = "*"），会跑在
 *     调用方线程（Tomcat / 定时任务 / MQ 消费线程）上 —— 这些线程都不归它管，它也没有清理时机
 *     （它只知道"要发消息了"，不知道调用方何时用完这条线程）。往这些线程的 MDC 里塞值，
 *     等于污染调用方后续日志（R 响应体、异常响应都从 MDC 取 requestId），并在线程池上必然串号。
 *   - 别把「生成 id」和「写 MDC」当成一件事：取不到 id 时这里会 **生成** 一个（给消息一个身份，
 *     合法且必要），但只写进消息头。写 MDC 的资格来自"我拥有这条线程"，
 *     而不是"我是数据的起点" —— 那是消费端的职责，见 MqConfig#receiveTraceIdInterceptor
 *     （它把 patterns 限定为 "*-in-*"，正是为了只写自己负责的消费线程）。
 *   - requestId 格式统一由 RequestIdUtil 生成，避免同一链路在不同环节出现两种格式
 */
public class TraceIdChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        // 1. 已有链路ID：保持原样，避免覆盖上游传下来的值
        if (message.getHeaders().containsKey(REQUEST_ID_HEADER)) return message;
        // 2. 取 MDC 中的链路ID，没有则生成（只用于写消息头，不回写 MDC）
        String traceId = RequestIdUtil.getOrCreate();

        // 3. Spring Messaging 的 Message 不可变，需要重新构建
        return MessageBuilder.fromMessage(message)
                .setHeader(REQUEST_ID_HEADER, traceId)
                .build();
    }
}
