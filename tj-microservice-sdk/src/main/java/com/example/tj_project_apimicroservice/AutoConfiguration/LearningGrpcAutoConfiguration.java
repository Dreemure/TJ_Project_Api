package com.example.tj_project_apimicroservice.AutoConfiguration;

import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;

@Configuration
@ImportGrpcClients(basePackages = "com.example.tj_project_apimicroservice.proto") // 指定 Stub 所在的包
public class LearningGrpcAutoConfiguration {
}
