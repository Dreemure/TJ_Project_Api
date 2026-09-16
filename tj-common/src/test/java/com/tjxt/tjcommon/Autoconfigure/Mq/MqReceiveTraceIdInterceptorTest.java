package com.tjxt.tjcommon.Autoconfigure.Mq;

import com.tjxt.tjcommon.Autoconfigure.VirtualThread.VirtualThreadConfig;
import com.tjxt.tjcommon.Utils.RequestIdUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.function.cloudevent.CloudEventsFunctionExtensionConfiguration;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.cloud.stream.function.FunctionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.util.Map;
import java.util.function.Consumer;

import static com.tjxt.tjcommon.Constants.Constant.REQUEST_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

/*
 * 消费端链路追踪拦截器的真实绑定验证（用 Spring Cloud Stream 官方 test-binder）。
 *
 * 为什么要用 test-binder 而不是直接 new 一个 DirectChannel：
 *   拦截器注册成了 @GlobalChannelInterceptor(patterns = "*-in-*")，它是否生效完全取决于
 *   「SCSt 到底把消费者通道命名成什么」。用真实的绑定基础设施才能证明
 *   输入通道名符合 <functionName>-in-<index> 约定（本用例直接断言 orderIn-in-0 存在）。
 *   如果哪天 SCSt 改了命名，或者有人自定义了通道名，本测试会失败 —— 这正是需要的提醒。
 *
 * 同时验证消费端的另外两条约定：
 *   1. 消息头有 requestId → 写进消费线程的 MDC（链路能接上）
 *   2. 消息头没有 requestId → 兜底生成，而不是沿用消费线程上的残留值（避免串号）
 */
class MqReceiveTraceIdInterceptorTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    // 函数目录（把 Consumer bean 变成函数定义）
                    ContextFunctionCatalogAutoConfiguration.class,
                    // 提供 FunctionInvocationHelper（SCSt 的 StreamBridge 依赖它）
                    CloudEventsFunctionExtensionConfiguration.class,
                    // SCSt 绑定基础设施：FunctionConfiguration 负责创建 <fn>-in-<index> 输入通道
                    BindingServiceConfiguration.class,
                    FunctionConfiguration.class,
                    // 官方 test-binder：提供 InputDestination，让测试能"假装"是 RabbitMQ
                    TestChannelBinderConfiguration.class))
            .withUserConfiguration(TestApp.class)
            .withPropertyValues(
                    "spring.cloud.function.definition=orderIn",
                    "spring.cloud.stream.bindings.orderIn-in-0.destination=order.topic",
                    "spring.cloud.stream.bindings.orderIn-in-0.group=order-service-group");

    @AfterEach
    void tearDown() {
        MDC.clear();
        TestApp.captured = null;
    }

    @Test
    @DisplayName("输入通道名符合 *-in-* 约定，且消费端拦截器把消息头 requestId 写进 MDC")
    void inputChannelShouldMatchPatternAndSetMdc() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            // 关键：*-in-* 这个 patterns 依赖 SCSt 的输入通道命名约定
            assertThat(context).hasBean("orderIn-in-0");

            InputDestination input = context.getBean(InputDestination.class);
            input.send(new GenericMessage<>("hello", Map.of(REQUEST_ID_HEADER, "trace-mq-1")));

            assertThat(TestApp.captured).isEqualTo("trace-mq-1");
        });
    }

    @Test
    @DisplayName("消息头没有 requestId 时兜底生成，不沿用消费线程上的残留值")
    void shouldGenerateInsteadOfKeepingStaleValue() {
        // 模拟"上一条消息处理完没清理干净"的消费线程
        MDC.put(REQUEST_ID_HEADER, "stale-from-previous-message");

        runner.run(context -> {
            assertThat(context).hasNotFailed();
            context.getBean(InputDestination.class).send(new GenericMessage<>("hello"));

            assertThat(TestApp.captured)
                    .as("必须生成新的 requestId，而不是沿用线程上的残留值")
                    .isNotNull()
                    .isNotEqualTo("stale-from-previous-message");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({MqConfig.class, VirtualThreadConfig.class})
    static class TestApp {

        /** 消费方法执行时读到的 requestId（由拦截器写入 MDC） */
        static volatile String captured;

        @Bean
        Consumer<Message<String>> orderIn() {
            return message -> captured = RequestIdUtil.get();
        }
    }
}
