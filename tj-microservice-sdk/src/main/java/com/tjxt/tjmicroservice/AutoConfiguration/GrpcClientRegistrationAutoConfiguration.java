package com.tjxt.tjmicroservice.AutoConfiguration;

import com.tjxt.tjmicroservice.proto.AuthServiceGrpc;
import com.tjxt.tjmicroservice.proto.CourseServiceGrpc;
import com.tjxt.tjmicroservice.proto.ExamServiceGrpc;
import com.tjxt.tjmicroservice.proto.LearningServiceGrpc;
import com.tjxt.tjmicroservice.proto.PromotionServiceGrpc;
import com.tjxt.tjmicroservice.proto.RemarkServiceGrpc;
import com.tjxt.tjmicroservice.proto.TradeServiceGrpc;
import com.tjxt.tjmicroservice.proto.UserServiceGrpc;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;

/*
 * gRPC 客户端（Stub）注册配置。
 * 职责：为每个下游服务显式声明"通道名"，把 Stub 绑定到对应通道。
 * 为什么不用包扫描：@ImportGrpcClients 的包扫描注册出来的 Stub 走的是**默认通道**
 * （spring.grpc.client.channel.default.target，默认 static://localhost:9090），
 * 无法为不同下游服务指定不同地址；显式写 target 才能把 Stub 绑定到
 * spring.grpc.client.channel.<target>.target 上（通道名 = 目标服务名）。
 * 说明：
 *   - 通道地址统一在 Nacos 的 shared-spring.yaml 中配置（spring.grpc.client.channel.*.target）
 *   - 各服务的 gRPC 服务端端口在各自 *-service.yaml 里配置（spring.grpc.server.port，约定 = HTTP 端口 + 1000）
 *   - 启动时会打印每个通道解析到的地址，缺配置会告警（见 GrpcChannelHealthLogger）
 */
@AutoConfiguration
public class GrpcClientRegistrationAutoConfiguration {

    /** 认证服务（角色、权限） */
    @Configuration
    @ImportGrpcClients(target = "auth-service", types = AuthServiceGrpc.AuthServiceBlockingStub.class)
    static class AuthClientConfig {
    }

    /** 用户服务 */
    @Configuration
    @ImportGrpcClients(target = "user-service", types = UserServiceGrpc.UserServiceBlockingStub.class)
    static class UserClientConfig {
    }

    /** 课程服务 */
    @Configuration
    @ImportGrpcClients(target = "course-service", types = CourseServiceGrpc.CourseServiceBlockingStub.class)
    static class CourseClientConfig {
    }

    /** 学习服务 */
    @Configuration
    @ImportGrpcClients(target = "learning-service", types = LearningServiceGrpc.LearningServiceBlockingStub.class)
    static class LearningClientConfig {
    }

    /** 考试服务 */
    @Configuration
    @ImportGrpcClients(target = "exam-service", types = ExamServiceGrpc.ExamServiceBlockingStub.class)
    static class ExamClientConfig {
    }

    /** 促销服务 */
    @Configuration
    @ImportGrpcClients(target = "promotion-service", types = PromotionServiceGrpc.PromotionServiceBlockingStub.class)
    static class PromotionClientConfig {
    }

    /** 备注（点赞/评价）服务 */
    @Configuration
    @ImportGrpcClients(target = "remark-service", types = RemarkServiceGrpc.RemarkServiceBlockingStub.class)
    static class RemarkClientConfig {
    }

    /** 交易服务 */
    @Configuration
    @ImportGrpcClients(target = "trade-service", types = TradeServiceGrpc.TradeServiceBlockingStub.class)
    static class TradeClientConfig {
    }
}
