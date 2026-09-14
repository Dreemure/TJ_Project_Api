package com.example.tjgateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@Slf4j
@EnableScheduling // 开启定时任务
@EnableDiscoveryClient
public class GatewayApplication {
    public static void main(String[] args) throws UnknownHostException {
        SpringApplication app = new SpringApplicationBuilder(GatewayApplication.class).build(args);
        Environment env = app.run(args).getEnvironment();
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        // log.info 使用的是 SLF4J 的占位符语法，{} 会按顺序被后面的参数替换
        log.info("""
                        --/
                        ---------------------------------------------------------------------------------------
                        \t\
                        Application '{}' is running! Access URLs:
                        \t\
                        Local: \t\t{}://localhost:{}
                        \t\
                        External: \t{}://{}:{}
                        \t\
                        Profile(s): \t{}
                        ---------------------------------------------------------------------------------------""",
                env.getProperty("spring.application.name"), // 读取配置
                protocol,
                env.getProperty("server.port"),
                protocol,
                InetAddress.getLocalHost().getHostAddress(),
                env.getProperty("server.port"),
                env.getActiveProfiles());
    }
}
