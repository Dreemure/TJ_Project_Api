package com.tjxt.tjgateway.Filter;

import com.tjxt.tjcommon.Constants.AuthConstants;
import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import com.tjxt.tjcommon.Model.Response.R;
import com.tjxt.tjcommon.Utils.RoleUtils;
import com.tjxt.tjgateway.Auth.JwtTokenVerifier;
import com.tjxt.tjgateway.Config.ExcludePathMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/*
 * 网关 JWT 认证过滤器（对应原项目 AccountAuthFilter 的“解析 token + 传递用户信息”职责）。
 * 职责：
 *   1. 从 authorization 请求头取出 token 并验签（支持裸 token 与 "Bearer xxx" 两种写法）
 *   2. 验签通过：把 LoginUserDTO 作为 principal、角色名作为权限写入 SecurityContext（响应式上下文）
 *   3. 验签失败：白名单路径直接放行（避免前端残留过期 token 导致无法登录），其余交给 401 入口
 * 说明：
 *   - 由 GatewaySecurityConfig 注册到 SecurityWebFilterChain 的认证阶段（AuthorizationWebFilter 之前）
 *   - 该过滤器不是 Spring Bean：避免被 Spring Boot 当作全局 WebFilter 重复注册（导致重复验签）
 *   - 下游服务的用户信息头由 UserInfoRelayFilter 统一注入
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    /** 验签结果在 exchange 上的属性名：供 JsonAuthenticationEntryPoint 输出精确业务码 */
    public static final String AUTH_RESULT_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".AUTH_RESULT";

    private final JwtTokenVerifier jwtTokenVerifier;
    private final ServerAuthenticationEntryPoint authenticationEntryPoint;
    private final ExcludePathMatcher excludePathMatcher;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String token = resolveToken(exchange.getRequest());
        if (!StringUtils.hasText(token)) {
            // 未携带 token：按匿名处理，是否放行由授权规则（白名单）决定
            return chain.filter(exchange);
        }

        R<LoginUserDTO> result = jwtTokenVerifier.verify(token);
        if (!result.success()) {
            if (excludePathMatcher.matches(exchange.getRequest().getMethod(), exchange.getRequest().getPath().value())) {
                log.debug("白名单路径忽略无效 token：{} {}", exchange.getRequest().getMethod(),
                        exchange.getRequest().getPath().value());
                return chain.filter(exchange);
            }
            exchange.getAttributes().put(AUTH_RESULT_ATTRIBUTE, result);
            return authenticationEntryPoint.commence(exchange,
                    new BadCredentialsException(result.getMsg()));
        }

        LoginUserDTO user = result.getData();
        List<GrantedAuthority> authorities = RoleUtils.resolve(user).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                .toList();
        var authentication = new UsernamePasswordAuthenticationToken(user, token, authorities);
        if (log.isDebugEnabled()) {
            log.debug("网关认证通过：userId={}, roleId={}, roles={}",
                    user.getUserId(), user.getRoleId(), authorities);
        }
        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    /**
     * 从请求头解析 token：兼容 "Bearer xxx" 与裸 token。
     */
    private String resolveToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(AuthConstants.AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(header)) {
            return null;
        }
        String token = header.trim();
        if (token.regionMatches(true, 0, AuthConstants.BEARER_PREFIX, 0, AuthConstants.BEARER_PREFIX.length())) {
            token = token.substring(AuthConstants.BEARER_PREFIX.length()).trim();
        }
        return StringUtils.hasText(token) ? token : null;
    }
}
