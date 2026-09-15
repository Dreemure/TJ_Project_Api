package com.tjxt.tjauth.Config;

import com.tjxt.tjauth.Filter.JwtAuthenticationFilter;
import com.tjxt.tjauth.Utils.AuthUtils;
import com.tjxt.tjcommon.Constants.ErrorInfo;
import com.tjxt.tjcommon.Model.Response.R;
import com.tjxt.tjcommon.Utils.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/*
 * 认证服务安全配置（Spring Security）。
 * 职责：
 *   1. 构建 SecurityFilterChain：无状态会话 + 关闭 CSRF/表单登录/HTTP Basic/默认登出
 *   2. 白名单放行（如 /accounts/login、/accounts/refresh、/jwks），其余请求必须携带有效 access token
 *   3. 401/403 统一返回 R 格式 JSON（而不是重定向登录页或浏览器弹框）
 * 说明：
 *   - 认证由 JwtAuthenticationFilter 完成（解析 authorization 头 → 写入 SecurityContext）
 *   - 动态路径级权限（路径→角色，数据存 auth 库并缓存到 Redis）由 AuthCheckAspect + AuthUtils 完成，
 *     两者互补：Security 负责“是否登录”，AuthUtils 负责“是否有该路径的权限”
 *   - Gateway 侧同样只做认证与白名单，角色级鉴权不再重复下沉
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthProperties authProperties;
    private final AuthUtils authUtils;

    /**
     * 认证服务安全过滤器链。
     *
     * @param http Security 的 HTTP 配置入口
     * @return 配置好的 SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String[] excludePaths = authProperties.getExcludePath().toArray(String[]::new);
        log.info("auth 服务鉴权已启用：白名单 {} 条 {}", excludePaths.length, authProperties.getExcludePath());

        return http
                // 无状态 JWT：不需要 CSRF、登录页、Basic 认证与默认登出
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // 不创建会话：身份完全来自 token
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 路径授权：白名单 + 预检请求放行，其余必须认证
                .authorizeHttpRequests(registry -> {
                    registry.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    if (excludePaths.length > 0) {
                        registry.requestMatchers(excludePaths).permitAll();
                    }
                    registry.anyRequest().authenticated();
                })
                // JWT 认证过滤器：位于用户名密码认证过滤器之前
                .addFilterBefore(new JwtAuthenticationFilter(authUtils), UsernamePasswordAuthenticationFilter.class)
                // 401 / 403 返回 R 格式 JSON
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(this::commence)
                        .accessDeniedHandler(this::handleAccessDenied))
                .build();
    }

    /**
     * 未认证（HTTP 401）。
     * <p>若请求经过 JwtAuthenticationFilter 且验签失败，则输出精确业务码（40101 已过期 / 40102 无效）。
     */
    private void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException ex) throws IOException {
        R<?> result = (R<?>) request.getAttribute(JwtAuthenticationFilter.AUTH_RESULT_ATTRIBUTE);
        int code = result == null ? ErrorInfo.Code.UNAUTHORIZED : result.getCode();
        String msg = result == null ? ErrorInfo.Msg.UNAUTHORIZED : result.getMsg();
        log.info("auth 服务鉴权失败：uri={}, code={}, msg={}", request.getRequestURI(), code, msg);
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, code, msg);
    }

    /**
     * 已登录但无权限（HTTP 403）。
     */
    private void handleAccessDenied(HttpServletRequest request, HttpServletResponse response,
                                    AccessDeniedException ex) throws IOException {
        log.info("auth 服务授权失败：uri={}, msg={}", request.getRequestURI(), ex.getMessage());
        writeJson(response, HttpServletResponse.SC_FORBIDDEN, ErrorInfo.Code.FORBIDDEN, ErrorInfo.Msg.FORBIDDEN);
    }

    /**
     * 写出 R 格式的 JSON 错误响应。
     */
    private void writeJson(HttpServletResponse response, int httpStatus, int code, String msg) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JsonUtils.toJsonStr(R.error(code, msg)));
    }
}
