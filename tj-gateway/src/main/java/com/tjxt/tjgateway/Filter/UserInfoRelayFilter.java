package com.tjxt.tjgateway.Filter;

import com.tjxt.tjcommon.Constants.AuthConstants;
import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import com.tjxt.tjcommon.Utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/*
 * 用户信息透传过滤器。
 * 职责：
 *   1. 无条件剥离客户端传入的 user-info 头（防止伪造身份）—— 该头只能由网关注入
 *   2. 已认证的请求：把 LoginUserDTO 以 Base64(JSON) 形式写入 user-info 头，供下游服务的 microservice-sdk 消费
 * 说明：
 *   - 顺序在 Spring Security 过滤器链（默认 -100）之后、网关路由过滤器之前，因此能读到认证结果
 *   - 下游 SDK（UserContextFilter）对该头做 Base64 解码 + JSON 反序列化，并写入 SecurityContext 与 UserContext
 *   - 与 gRPC 透传（UserRelayClientInterceptor 用明文 JSON）格式不同，两者互不影响
 */
@Slf4j
@Component
public class UserInfoRelayFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 1. 剥离客户端伪造的 user-info
        ServerHttpRequest stripped = exchange.getRequest().mutate()
                .headers(headers -> headers.remove(AuthConstants.USER_HEADER))
                .build();
        ServerWebExchange mutated = exchange.mutate().request(stripped).build();

        // 2. 认证通过则注入用户信息
        return ReactiveSecurityContextHolder.getContext()
                .handle((context, sink) -> {
                    Authentication authentication = context.getAuthentication();
                    if (authentication != null && authentication.getPrincipal() instanceof LoginUserDTO user) {
                        sink.next(user);
                    }
                })
                .cast(LoginUserDTO.class)
                .flatMap(user -> chain.filter(withUserInfo(mutated, user)))
                .switchIfEmpty(Mono.defer(() -> chain.filter(mutated)));
    }

    /**
     * 把用户信息写入请求头。
     */
    private ServerWebExchange withUserInfo(ServerWebExchange exchange, LoginUserDTO user) {
        String value = encode(user);
        return exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header(AuthConstants.USER_HEADER, value)
                        .build())
                .build();
    }

    /**
     * Base64(JSON(LoginUserDTO))：请求头只能放 ASCII 字符，故 Base64 编码。
     */
    private String encode(LoginUserDTO user) {
        String json = JsonUtils.toJsonStr(user);
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int getOrder() {
        // Spring Security 过滤器链默认顺序为 -100（SecurityProperties.DEFAULT_FILTER_ORDER），
        // 网关路由过滤器顺序为 LOWEST_PRECEDENCE；取 0 表示“认证之后、路由之前”
        return 0;
    }
}
