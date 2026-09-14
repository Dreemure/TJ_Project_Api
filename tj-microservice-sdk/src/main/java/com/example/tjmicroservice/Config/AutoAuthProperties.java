package com.example.tjmicroservice.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

/*
 * SDK 自动鉴权的白名单配置。
 * 职责：读取 tj.auth.exclude-path，业务服务在 Nacos 中配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "tj.auth")
public class AutoAuthProperties {
    /** 无需认证的白名单路径 */
    private Set<String> excludePath = new HashSet<>();

    /** 默认白名单（所有服务共用） */
    public static final Set<String> DEFAULT_EXCLUDE = Set.of(
            "/error/**",
            "/actuator/**",
            "/v3/api-docs/**",
            "/favicon.ico"
    );

    /** 获取最终白名单 */
    public Set<String> getFinalExcludePath() {
        Set<String> result = new HashSet<>(DEFAULT_EXCLUDE);
        if (excludePath != null) {
            result.addAll(excludePath);
        }
        return result;
    }
}
