package com.tjxt.tjmicroservice.AutoConfiguration;

import com.tjxt.tjmicroservice.proto.AuthServiceGrpc;
import com.tjxt.tjmicroservice.proto.CourseServiceGrpc;
import com.tjxt.tjmicroservice.proto.ExamServiceGrpc;
import com.tjxt.tjmicroservice.proto.LearningServiceGrpc;
import com.tjxt.tjmicroservice.proto.PromotionServiceGrpc;
import com.tjxt.tjmicroservice.proto.RemarkServiceGrpc;
import com.tjxt.tjmicroservice.proto.TradeServiceGrpc;
import com.tjxt.tjmicroservice.proto.UserServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * gRPC 通道自检（启动完成后执行）。
 * 职责：
 *   1. 打印本服务 gRPC 服务端端口（spring.grpc.server.port）
 *   2. 打印各 gRPC 客户端通道解析到的地址，并对"没配地址、仍在用默认 static://localhost:9090"的通道告警
 * 背景：
 *   - 客户端通道地址来自 spring.grpc.client.channel.<通道名>.target（Nacos shared-spring.yaml 已统一配置）
 *   - 通道名 = 目标服务名，由 GrpcClientRegistrationAutoConfiguration 显式绑定
 *     （包扫描方式只会走默认通道，无法为不同下游指定地址）
 *   - 目标服务的 gRPC 端口在各自 *-service.yaml 配置（约定 = HTTP 端口 + 1000）
 */
@Slf4j
public class GrpcChannelHealthLogger implements ApplicationListener<ApplicationReadyEvent> {

    /** 没配 channel.target 时 Spring gRPC 的默认目标 */
    private static final String DEFAULT_HOST_PORT = "localhost:9090";

    /** 客户端通道名（= 目标服务名）→ 对应 Stub 类型（日志里用于说明用途） */
    private static final Map<String, Class<?>> CLIENT_CHANNELS = new LinkedHashMap<>();

    static {
        CLIENT_CHANNELS.put("auth-service", AuthServiceGrpc.AuthServiceBlockingStub.class);
        CLIENT_CHANNELS.put("user-service", UserServiceGrpc.UserServiceBlockingStub.class);
        CLIENT_CHANNELS.put("course-service", CourseServiceGrpc.CourseServiceBlockingStub.class);
        CLIENT_CHANNELS.put("learning-service", LearningServiceGrpc.LearningServiceBlockingStub.class);
        CLIENT_CHANNELS.put("exam-service", ExamServiceGrpc.ExamServiceBlockingStub.class);
        CLIENT_CHANNELS.put("promotion-service", PromotionServiceGrpc.PromotionServiceBlockingStub.class);
        CLIENT_CHANNELS.put("remark-service", RemarkServiceGrpc.RemarkServiceBlockingStub.class);
        CLIENT_CHANNELS.put("trade-service", TradeServiceGrpc.TradeServiceBlockingStub.class);
    }

    private final ApplicationContext context;
    private final Environment environment;
    private final GrpcClientProperties clientProperties;

    public GrpcChannelHealthLogger(ApplicationContext context,
                                   Environment environment,
                                   ObjectProvider<GrpcClientProperties> clientPropertiesProvider) {
        this.context = context;
        this.environment = environment;
        this.clientProperties = clientPropertiesProvider.getIfAvailable();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 1. 服务端端口
        String serverPort = environment.getProperty("spring.grpc.server.port");
        log.info("gRPC 服务端端口：{}（spring.grpc.server.port；不配置时默认 9090，多服务同机会冲突）",
                serverPort == null ? "未配置" : serverPort);

        // 2. 客户端通道地址
        if (clientProperties == null) {
            log.warn("未引入 spring-boot-starter-grpc-client，gRPC 客户端不可用");
            return;
        }
        Map<String, GrpcClientProperties.Channel> channels = clientProperties.getChannel();
        List<String> ready = new ArrayList<>();
        List<String> defaulted = new ArrayList<>();
        CLIENT_CHANNELS.forEach((name, stubType) -> {
            GrpcClientProperties.Channel channel = channels.get(name);
            String target = channel == null ? null : channel.getTarget();
            if (target == null || target.isBlank() || DEFAULT_HOST_PORT.equals(clean(target))) {
                defaulted.add(name);
                return;
            }
            boolean stubPresent = !context.getBeansOfType(stubType).isEmpty();
            ready.add("%s -> %s%s".formatted(name, target, stubPresent ? "" : "（StubBean 缺失）"));
        });
        if (!ready.isEmpty()) {
            log.info("gRPC 客户端通道就绪：{}", ready);
        }
        if (!defaulted.isEmpty()) {
            log.warn("""
                    以下 gRPC 客户端通道未配置地址，会退回默认 static://{}，跨服务调用将失败：
                      {}
                    请在 Nacos shared-spring.yaml 补（通道名 = 目标服务名）：
                      spring:
                        grpc:
                          client:
                            channel:
                              <通道名>:
                                target: static://<host>:<gRPC端口>
                    目标服务需同时配置 spring.grpc.server.port（约定 = HTTP 端口 + 1000）。
                    """, DEFAULT_HOST_PORT, defaulted);
        }
    }

    /** 去掉协议前缀后比较，兼容 static://localhost:9090 与 localhost:9090 两种写法 */
    private String clean(String target) {
        String result = target.trim();
        for (String prefix : List.of("static://", "static:", "tcp://", "tcp:")) {
            if (result.startsWith(prefix)) {
                result = result.substring(prefix.length());
            }
        }
        return result;
    }
}
