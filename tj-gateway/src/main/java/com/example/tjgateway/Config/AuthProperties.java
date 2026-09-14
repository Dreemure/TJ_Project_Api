package com.example.tjgateway.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/*
 * 认证配置属性。
 * 职责：从 application.yml 的 tj.auth.* 读取认证相关配置，包含无需认证的路径白名单。
 * 说明：白名单 = yml 中配置的自定义路径 + 内置的默认路径（登录、刷新 token 等）。
 * 使用：通过依赖注入获取，调用 getExcludePath() 读取最终白名单。
 */
@Data
@Component
@ConfigurationProperties(prefix = "tj.auth")
public class AuthProperties {

    /**
     * 用户配置的额外白名单路径（不含默认路径）。
     * 若 yml 未配置，保持为空集合，避免 NPE。
     */
    private Set<String> excludePath = new HashSet<>();

    /**
     * 内置的默认白名单路径，用户不可覆盖。
     */
    private static final Set<String> DEFAULT_EXCLUDE_PATHS = Set.of(
            "/error/**",
            "/jwks",
            "/accounts/login",
            "/accounts/admin/login",
            "/accounts/refresh"
    );

    /**
     * 获取最终白名单：用户配置 + 内置默认。
     *
     * @return 合并后的白名单路径集合
     */
    public Set<String> getFinalExcludePath() {
        Set<String> result = new HashSet<>(DEFAULT_EXCLUDE_PATHS);
        if (excludePath != null) {
            result.addAll(excludePath);
        }
        return result;
    }
}