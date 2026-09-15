package com.example.tjauth.Config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/*
 * 认证配置属性（auth 服务专属）。
 * 职责：绑定 Nacos 中 auth-service.yaml 的 tj.auth.* 配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "tj.auth")
public class AuthProperties {

    /** JWT 相关配置 */
    private Jwt jwt = new Jwt();

    /** 请求头名称 */
    private Headers headers = new Headers();

    /** 无需认证的白名单路径 */
    private Set<String> excludePath = new HashSet<>();

    @Getter
    @Setter
    public static class Jwt {
        /** 私钥路径 */
        private String privateKeyPath;
        /** 公钥路径 */
        private String publicKeyPath;
        /** 算法（默认 RS256） */
        private String algorithm = "RS256";
        /** 签发者 */
        private String issuer = "tj-auth";
        /** Access Token 有效期 */
        private Duration tokenTtl = Duration.ofMinutes(30);
        /** Refresh Token 有效期 */
        private Duration refreshTtl = Duration.ofDays(7);
        private Duration rememberMeTtl = Duration.ofDays(7);
    }

    @Getter
    @Setter
    public static class Headers {
        private String authorization = "Authorization";
        private String refresh = "Refresh";
    }
}
