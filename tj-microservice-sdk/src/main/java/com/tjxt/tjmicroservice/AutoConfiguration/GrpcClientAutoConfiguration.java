package com.tjxt.tjmicroservice.AutoConfiguration;

import com.tjxt.tjmicroservice.Client.AuthGrpcClient;
import com.tjxt.tjmicroservice.Client.CourseGrpcClient;
import com.tjxt.tjmicroservice.Client.ExamGrpcClient;
import com.tjxt.tjmicroservice.Client.LearningGrpcClient;
import com.tjxt.tjmicroservice.Client.PromotionGrpcClient;
import com.tjxt.tjmicroservice.Client.RemarkGrpcClient;
import com.tjxt.tjmicroservice.Client.TradeGrpcClient;
import com.tjxt.tjmicroservice.Client.UserGrpcClient;
import com.tjxt.tjmicroservice.proto.AuthServiceGrpc;
import com.tjxt.tjmicroservice.proto.CourseServiceGrpc;
import com.tjxt.tjmicroservice.proto.ExamServiceGrpc;
import com.tjxt.tjmicroservice.proto.LearningServiceGrpc;
import com.tjxt.tjmicroservice.proto.PromotionServiceGrpc;
import com.tjxt.tjmicroservice.proto.RemarkServiceGrpc;
import com.tjxt.tjmicroservice.proto.TradeServiceGrpc;
import com.tjxt.tjmicroservice.proto.UserServiceGrpc;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/*
 * gRPC 客户端包装类自动配置。
 * 职责：把 SDK 中的 8 个 gRPC 客户端包装类（*GrpcClient）注册为 Spring Bean。
 * 说明：
 *   - Stub（桩）由 @ImportGrpcClients 扫描 com.tjxt.tjmicroservice.proto 生成，见 LearningGrpcAutoConfiguration；
 *     本类在其之后加载（@AutoConfiguration(after = ...)），保证判断条件能看到 Stub Bean 定义
 *   - 每个包装类都用 @ConditionalOnBean(对应 Stub) 守卫：业务服务只依赖部分服务（或未配置某个通道）时，
 *     不会因为缺少 Stub 而启动失败；需要时再补 spring.grpc.client.channels.* 配置即可
 *   - 使用 @ConditionalOnMissingBean：业务方可以自定义同类型 Bean 覆盖 SDK 的默认实现
 * 使用：注入对应客户端即可，例如
 *   <pre>private final UserGrpcClient userGrpcClient;</pre>
 */
@AutoConfiguration(after = GrpcClientRegistrationAutoConfiguration.class)
public class GrpcClientAutoConfiguration {

    /**
     * 启动后打印各 gRPC 通道解析到的地址；缺配置（仍指向默认 localhost:9090）时给出告警与修复指引。
     */
    @Bean
    @ConditionalOnClass(GrpcClientProperties.class)
    public GrpcChannelHealthLogger grpcChannelHealthLogger(ApplicationContext context,
                                                           Environment environment,
                                                           ObjectProvider<GrpcClientProperties> clientProperties) {
        return new GrpcChannelHealthLogger(context, environment, clientProperties);
    }

    /** 认证服务客户端（角色、权限等） */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AuthServiceGrpc.AuthServiceBlockingStub.class)
    public AuthGrpcClient authGrpcClient(AuthServiceGrpc.AuthServiceBlockingStub stub) {
        return new AuthGrpcClient(stub);
    }

    /** 用户服务客户端 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(UserServiceGrpc.UserServiceBlockingStub.class)
    public UserGrpcClient userGrpcClient(UserServiceGrpc.UserServiceBlockingStub stub) {
        return new UserGrpcClient(stub);
    }

    /** 课程服务客户端 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(CourseServiceGrpc.CourseServiceBlockingStub.class)
    public CourseGrpcClient courseGrpcClient(CourseServiceGrpc.CourseServiceBlockingStub stub) {
        return new CourseGrpcClient(stub);
    }

    /** 学习服务客户端 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(LearningServiceGrpc.LearningServiceBlockingStub.class)
    public LearningGrpcClient learningGrpcClient(LearningServiceGrpc.LearningServiceBlockingStub stub) {
        return new LearningGrpcClient(stub);
    }

    /** 考试服务客户端 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ExamServiceGrpc.ExamServiceBlockingStub.class)
    public ExamGrpcClient examGrpcClient(ExamServiceGrpc.ExamServiceBlockingStub stub) {
        return new ExamGrpcClient(stub);
    }

    /** 促销服务客户端 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(PromotionServiceGrpc.PromotionServiceBlockingStub.class)
    public PromotionGrpcClient promotionGrpcClient(PromotionServiceGrpc.PromotionServiceBlockingStub stub) {
        return new PromotionGrpcClient(stub);
    }

    /** 备注（点赞/评价）服务客户端 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RemarkServiceGrpc.RemarkServiceBlockingStub.class)
    public RemarkGrpcClient remarkGrpcClient(RemarkServiceGrpc.RemarkServiceBlockingStub stub) {
        return new RemarkGrpcClient(stub);
    }

    /** 交易服务客户端 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(TradeServiceGrpc.TradeServiceBlockingStub.class)
    public TradeGrpcClient tradeGrpcClient(TradeServiceGrpc.TradeServiceBlockingStub stub) {
        return new TradeGrpcClient(stub);
    }
}
