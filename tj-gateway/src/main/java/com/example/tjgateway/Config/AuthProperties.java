package com.example.tjgateway.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/*
 * auth 服务认证配置。
 * 职责：绑定 tj.auth.* 配置，提供 SecurityFilterChain 使用的白名单。
 * 说明：白名单完全由 yml 的 exclude-path 提供，代码不内置默认值。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tj.auth")
public class AuthProperties {

    /** 免认证路径白名单，由 yml 配置。 */
    private Set<String> excludePath = new HashSet<>();
}