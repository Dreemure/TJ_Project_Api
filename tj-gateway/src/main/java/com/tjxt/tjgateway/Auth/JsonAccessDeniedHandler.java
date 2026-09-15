package com.tjxt.tjgateway.Auth;

import com.tjxt.tjcommon.Constants.ErrorInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/*
 * 网关无权限处理器（HTTP 403）。
 * 职责：已登录但权限不足时返回 R 格式的 403 JSON。
 * 说明：网关当前只做“认证 + 白名单”，角色级授权由下游服务（SDK + Spring Security）承担；
 *       此处为后续在网关做路径级鉴权（对应原项目 AuthUtil#checkAuth）预留统一出口。
 */
@Slf4j
@Component
public class JsonAccessDeniedHandler implements ServerAccessDeniedHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        log.info("网关授权失败：uri={}, msg={}", exchange.getRequest().getURI().getPath(), denied.getMessage());
        return AuthResponseWriter.write(exchange, HttpStatus.FORBIDDEN,
                ErrorInfo.Code.FORBIDDEN, ErrorInfo.Msg.FORBIDDEN);
    }
}
