package com.tjxt.tjgateway.Config;

import com.tjxt.tjgateway.Auth.JsonAccessDeniedHandler;
import com.tjxt.tjgateway.Auth.JsonAuthenticationEntryPoint;
import com.tjxt.tjgateway.Auth.JwtTokenVerifier;
import com.tjxt.tjgateway.Auth.PrivilegeAuthorizationManager;
import com.tjxt.tjgateway.Filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import java.util.Set;

/*
 * 网关安全配置（响应式 Spring Security）。
 * 职责：构建 SecurityWebFilterChain —— 白名单放行 + 其余请求"认证 + 路径级鉴权" + 401/403 返回 R 格式 JSON。
 * 说明：
 *   - 对应原项目 AccountAuthFilter（GlobalFilter）的职责，改用 Spring Security 的过滤器链实现：
 *     认证由 JwtAuthenticationFilter 完成，授权规则由 authorizeExchange 完成，
 *     路径级鉴权（方法+路径 → 角色）由 PrivilegeAuthorizationManager 完成（Spring Security 的 ReactiveAuthorizationManager），
 *     错误输出由认证入口/无权限处理器完成
 *   - 无状态：securityContextRepository 使用 NoOp，不创建会话，身份完全来自 token
 *   - 鉴权分层（Spring Security 视角）：
 *       网关：认证 + 白名单 + 粗粒度路径/角色鉴权（权限表来自 Redis，运营可配）
 *       业务服务：@PreAuthorize 等细粒度鉴权（SDK 已把 ROLE_xxx 与用户信息放进 SecurityContext）
 *   - 白名单见 tj.auth.exclude-path（application.yaml / Nacos），权限表见 tj.auth.privilege.*
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final AuthProperties authProperties;
    private final JwtTokenVerifier jwtTokenVerifier;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final PrivilegeAuthorizationManager privilegeAuthorizationManager;

    /**
     * 网关安全过滤器链。
     *
     * @param http ServerHttpSecurity 配置入口
     * @return 配置好的 SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // 白名单 = 代码兜底 + 配置项（见 AuthProperties#getFinalExcludePath）
        Set<String> excludePaths = authProperties.getFinalExcludePath();
        ExcludePathMatcher excludePathMatcher = new ExcludePathMatcher(excludePaths);
        log.info("网关鉴权已启用：白名单 {} 条 {}，路径级鉴权 {}",
                excludePathMatcher.size(), excludePaths,
                authProperties.getPrivilege().isEnabled() ? "开启" : "关闭");

        return http
                // 无状态 JWT：不需要 CSRF / 登录页 / Basic 认证 / 默认登出
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                // 不创建会话、不把 SecurityContext 写入 session
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                // 授权规则：白名单 + 预检请求放行，其余交给鉴权管理器（认证 + 权限表校验）
                .authorizeExchange(exchange -> {
                    exchange.pathMatchers(HttpMethod.OPTIONS).permitAll();
                    if (!excludePathMatcher.isEmpty()) {
                        String[] plainPatterns = excludePathMatcher.plainPatterns();
                        if (plainPatterns.length > 0) {
                            exchange.pathMatchers(plainPatterns).permitAll();
                        }
                        excludePathMatcher.methodPatterns().forEach((method, patterns) ->
                                exchange.pathMatchers(method, patterns.toArray(String[]::new)).permitAll());
                    }
                    exchange.anyExchange().access(privilegeAuthorizationManager);
                })
                // 401 / 403 统一返回 R 格式 JSON
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // JWT 认证过滤器：接在认证阶段（授权过滤器的前面）
                .addFilterAt(new JwtAuthenticationFilter(jwtTokenVerifier, authenticationEntryPoint, excludePathMatcher),
                        SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
