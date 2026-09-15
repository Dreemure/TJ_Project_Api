package com.example.tjgateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


@SpringBootApplication
@Slf4j
@EnableScheduling // 开启定时任务
@EnableDiscoveryClient
public class GatewayApplication {

    private static final String SEPARATOR = "-".repeat(88);

    void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(GatewayApplication.class, args);
        printStartupInfo(ctx.getEnvironment());
    }

    private static void printStartupInfo(Environment env) {
        String appName   = env.getProperty("spring.application.name", "unknown");
        String port      = env.getProperty("server.port", "8080");
        String protocol  = env.getProperty("server.ssl.key-store") != null ? "https" : "http";
        String profiles  = String.join(",", env.getActiveProfiles());
        String localUrl  = "%s://localhost:%s".formatted(protocol, port);

        List<String> lines = new ArrayList<>();
        lines.add(SEPARATOR);
        lines.add("  Application '%s' is running!".formatted(appName));
        lines.add("");
        lines.add("  Local:     " + localUrl);
        lines.add("  External:  %s://%s:%s".formatted(protocol, resolveHost(), port));
        lines.add("  Profile:   %s".formatted(profiles.isEmpty() ? "default" : profiles));
        lines.add(SEPARATOR);

        lines.forEach(log::info);
    }

    private static String resolveHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}