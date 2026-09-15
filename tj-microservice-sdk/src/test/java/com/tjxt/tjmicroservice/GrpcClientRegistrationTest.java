package com.tjxt.tjmicroservice;

import com.tjxt.tjmicroservice.AutoConfiguration.GrpcClientAutoConfiguration;
import com.tjxt.tjmicroservice.AutoConfiguration.GrpcClientRegistrationAutoConfiguration;
import com.tjxt.tjmicroservice.Client.CourseGrpcClient;
import com.tjxt.tjmicroservice.Client.UserGrpcClient;
import com.tjxt.tjmicroservice.proto.CourseServiceGrpc;
import com.tjxt.tjmicroservice.proto.UserServiceGrpc;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/*
 * gRPC 客户端注册与通道地址回归测试。
 * 验证：
 *   1. SDK 显式声明的通道名（@ImportGrpcClients(target = 服务名)）生效：
 *      每个下游 Stub 使用 spring.grpc.client.channel.<服务名>.target 的地址，而不是默认的 localhost:9090
 *   2. 对应的 *GrpcClient 包装类（GrpcClientAutoConfiguration）按 Stub 是否注册装配
 * 背景：包扫描方式注册的 Stub 会走「默认通道」，无法按下游服务区分地址（这是本次改成显式 target 的原因）。
 */
@SpringBootTest(classes = GrpcClientRegistrationTest.App.class, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "spring.grpc.client.channel.user-service.target=static://127.0.0.1:11012",
        "spring.grpc.client.channel.course-service.target=static://127.0.0.1:11013"
})
class GrpcClientRegistrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({GrpcClientRegistrationAutoConfiguration.class, GrpcClientAutoConfiguration.class})
    static class App {
    }

    @Autowired
    private UserServiceGrpc.UserServiceBlockingStub userStub;
    @Autowired
    private CourseServiceGrpc.CourseServiceBlockingStub courseStub;
    @Autowired
    private UserGrpcClient userGrpcClient;
    @Autowired
    private CourseGrpcClient courseGrpcClient;

    @Test
    @DisplayName("每个下游服务使用自己通道配置的地址（不是默认 localhost:9090）")
    void eachClientUsesItsOwnChannelTarget() {
        assertEquals("127.0.0.1:11012", authority(userStub));
        assertEquals("127.0.0.1:11013", authority(courseStub));
    }

    @Test
    @DisplayName("Stub 存在时对应的 *GrpcClient 包装类被装配")
    void grpcClientWrappersAreRegistered() {
        assertNotNull(userGrpcClient);
        assertNotNull(courseGrpcClient);
    }

    private String authority(Object stub) {
        try {
            var method = stub.getClass().getMethod("getChannel");
            Object channel = method.invoke(stub);
            return channel instanceof ManagedChannel managed ? managed.authority() : String.valueOf(channel);
        } catch (Exception e) {
            throw new IllegalStateException("读取 Stub 通道失败: " + e.getMessage(), e);
        }
    }
}
