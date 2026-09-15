package com.tjxt.tjmicroservice.AutoConfiguration;


import com.tjxt.tjcommon.Constants.ErrorInfo;
import com.tjxt.tjcommon.Model.Response.R;
import com.tjxt.tjcommon.Utils.JsonUtils;
import com.tjxt.tjmicroservice.Config.AutoAuthProperties;
import com.tjxt.tjmicroservice.Filter.UserContextFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


/*
 * 用户上下文自动配置。
 * 职责：
 *   1. 注册 UserContextFilter（从请求头解析用户信息）
 *   2. 提供默认的 SecurityFilterChain（无状态 + 白名单 + JSON 401/403）
 * 说明：
 *   - 仅在 Servlet 环境生效（@ConditionalOnWebApplication(type = SERVLET)）
 *   - 业务服务可通过自定义 SecurityFilterChain Bean 覆盖默认配置
 *   - 白名单通过 tj.auth.exclude-path 配置
 */
@Configuration
@EnableWebSecurity                                                      // 启用 Security 的 Web 支持
@EnableMethodSecurity                                                   // 启用方法级注解（@PreAuthorize 等）
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)   // 仅 Servlet 环境生效（网关 WebFlux 不加载）
@ConditionalOnClass({SecurityFilterChain.class, Filter.class})          // classpath 有 Security 才加载
@EnableConfigurationProperties(AutoAuthProperties.class)                // 绑定 tj.auth.* 配置
public class UserContextAutoConfiguration {

    /**
     * 注册用户上下文过滤器。
     * <p>职责：从请求头 user-info 解析用户信息，放入 Security 上下文和 UserContext。
     * <p>说明：业务服务可自定义 UserContextFilter Bean 覆盖本配置。
     */
    @Bean
    @ConditionalOnMissingBean
    public UserContextFilter userContextFilter() {
        return new UserContextFilter();
    }

    /**
     * 默认 Security 过滤器链。
     * <p>行为：
     *   - 关闭 CSRF / formLogin / httpBasic / logout（前后端分离）
     *   - 无状态会话（STATELESS，不创建 HttpSession）
     *   - 白名单放行（含 OPTIONS 预检请求）
     *   - 其他请求需认证
     *   - UserContextFilter 挂在 UsernamePasswordAuthenticationFilter 之前
     *   - 401/403 返回 JSON 而非重定向
     * <p>说明：若业务服务自定义 SecurityFilterChain Bean，本配置自动失效。
     *
     * @param http          Security 的 HTTP 配置入口
     * @param authProperties 白名单配置（来自 tj.auth.exclude-path）
     * @param userContextFilter 用户上下文过滤器
     * @return 配置好的 SecurityFilterChain
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            AutoAuthProperties authProperties,
            UserContextFilter userContextFilter) throws Exception {

        return http
                // 关闭 CSRF：无状态 JWT 不需要 CSRF token
                .csrf(AbstractHttpConfigurer::disable)
                // 关闭默认登录页：前后端分离不需要
                .formLogin(AbstractHttpConfigurer::disable)
                // 关闭 HTTP Basic：避免浏览器弹出认证框
                .httpBasic(AbstractHttpConfigurer::disable)
                // 关闭默认登出页：不需要
                .logout(AbstractHttpConfigurer::disable)
                // 无状态会话：不创建 HttpSession
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 路径授权
                .authorizeHttpRequests(auth -> auth
                        // 白名单放行（用户配置 + SDK 默认）
                        .requestMatchers(authProperties.getFinalExcludePath().toArray(new String[0])).permitAll()
                        // CORS 预检请求放行
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated())
                // 用户上下文过滤器挂在认证过滤器之前（保证后续能拿到用户信息）
                .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class)
                // 401/403 返回 JSON，而不是重定向到登录页
                .exceptionHandling(ex -> ex
                        // 未登录：返回 401 JSON
                        .authenticationEntryPoint((req, resp, e) -> writeJson(resp, HttpServletResponse.SC_UNAUTHORIZED,
                                ErrorInfo.Code.UNAUTHORIZED, ErrorInfo.Msg.UNAUTHORIZED))
                        // 无权限：返回 403 JSON
                        .accessDeniedHandler((req, resp, e) -> writeJson(resp, HttpServletResponse.SC_FORBIDDEN,
                                ErrorInfo.Code.FORBIDDEN, ErrorInfo.Msg.FORBIDDEN)))
                .build();
    }

    /**
     * 写出 R 格式的 JSON 响应（与网关、auth 服务的错误格式一致）。
     *
     * @param resp       HTTP 响应
     * @param httpStatus HTTP 状态码
     * @param code       业务码
     * @param msg        提示消息
     */
    private void writeJson(HttpServletResponse resp, int httpStatus, int code, String msg) throws IOException {
        resp.setStatus(httpStatus);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write(JsonUtils.toJsonStr(R.error(code, msg)));
    }
}