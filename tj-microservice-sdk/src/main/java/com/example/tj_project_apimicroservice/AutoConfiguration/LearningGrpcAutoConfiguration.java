package com.example.tj_project_apimicroservice.AutoConfiguration;

import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;

/*
 * gRPC 客户端自动配置类。
 * 职责：通过 @ImportGrpcClients 扫描指定包下的 gRPC Stub 并注册为 Spring Bean，
 *       使业务代码可通过 @Autowired 直接注入 Stub 调用远程服务。
 * 注意：需配合 application.yml 中 spring.grpc.client.channels.* 配置目标服务地址。
 */
@Configuration
@ImportGrpcClients(basePackages = "com.example.tj_project_apimicroservice.proto") // 指定 Stub 所在的包
public class LearningGrpcAutoConfiguration {
}
