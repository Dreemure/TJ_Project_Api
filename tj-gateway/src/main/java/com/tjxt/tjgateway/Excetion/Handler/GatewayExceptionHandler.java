package com.tjxt.tjgateway.Excetion.Handler;

import com.tjxt.tjcommon.Constants.Constant;
import com.tjxt.tjcommon.Constants.ErrorInfo;
import com.tjxt.tjcommon.Exceptions.CommonException;
import com.tjxt.tjcommon.Exceptions.UnauthorizedException;
import com.tjxt.tjcommon.Model.Response.R;
import com.tjxt.tjcommon.Utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static com.tjxt.tjcommon.Constants.ErrorInfo.Code.FAILED;
import static com.tjxt.tjcommon.Constants.ErrorInfo.Msg.SERVER_INTER_ERROR;

/*
 * 网关全局异常处理器。
 * 职责：捕获网关处理过程中抛出的所有异常，统一翻译为 R<T> 格式的 JSON 响应。
 * 说明：
 *   - 实现 ErrorWebExceptionHandler，优先级设为最高（HIGHEST_PRECEDENCE）
 *   - 401（UnauthorizedException）保留原始状态码，以 JSON 形式返回
 *   - 5xx / 未知异常统一返回 500，并记录完整堆栈日志
 *   - requestId 从请求头获取（由网关入口的 RequestIdWebFilter 注入）
 */
@Slf4j
@Component
public class GatewayExceptionHandler implements ErrorWebExceptionHandler, Ordered {

    @Override
    public @NonNull Mono<Void> handle(ServerWebExchange exchange, @NonNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        // 1. 响应已提交，无法再修改，交给默认处理
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 2. 翻译异常为 HTTP 状态码 + 业务消息
        HttpStatus status;
        int bizCode = FAILED;
        String message;

        if (ex instanceof UnauthorizedException e) {
            // 未登录：保留原始 HTTP 状态码（通常 401）
            status = HttpStatus.valueOf(e.getStatus());
            bizCode = e.getCode();
            message = e.getMessage();
            log.info("网关鉴权失败: uri={}, message={}",
                    exchange.getRequest().getURI().getPath(), message);

        } else if (ex instanceof CommonException e) {
            // 自定义业务异常
            status = HttpStatus.valueOf(e.getStatus());
            bizCode = e.getCode();
            message = e.getMessage();
            log.warn("网关业务异常: uri={}, code={}, message={}",
                    exchange.getRequest().getURI().getPath(), bizCode, message);

        } else if (ex instanceof AuthenticationException) {
            // Spring Security 认证异常（兜底：正常情况下由 JsonAuthenticationEntryPoint 处理）
            status = HttpStatus.UNAUTHORIZED;
            bizCode = ErrorInfo.Code.UNAUTHORIZED;
            message = ErrorInfo.Msg.UNAUTHORIZED;
            log.info("网关认证异常: uri={}, message={}",
                    exchange.getRequest().getURI().getPath(), ex.getMessage());

        } else if (ex instanceof AccessDeniedException) {
            // Spring Security 授权异常（兜底：正常情况下由 JsonAccessDeniedHandler 处理）
            status = HttpStatus.FORBIDDEN;
            bizCode = ErrorInfo.Code.FORBIDDEN;
            message = ErrorInfo.Msg.FORBIDDEN;
            log.info("网关授权异常: uri={}", exchange.getRequest().getURI().getPath());

        } else if (ex instanceof ResponseStatusException e) {
            // Spring 原生状态异常（404、403 等）
            status = HttpStatus.valueOf(e.getStatusCode().value());
            message = e.getReason() != null ? e.getReason() : status.getReasonPhrase();
            log.info("网关状态异常: uri={}, status={}",
                    exchange.getRequest().getURI().getPath(), status.value());

        } else {
            // 未知异常：兜底 500
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = SERVER_INTER_ERROR;
            writeErrorLog(exchange, ex);
        }

        // 3. 写出 JSON 响应
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        R<Void> body = R.error(bizCode, message);
        // requestId 从请求头取（网关入口注入的）
        String requestId = exchange.getRequest()
                .getHeaders()
                .getFirst(Constant.REQUEST_ID_HEADER);
        if (requestId != null) {
            body.requestId(requestId);
        }

        byte[] resp = JsonUtils.toJsonStr(body).getBytes(StandardCharsets.UTF_8);
        return response.writeWith(
                Mono.fromSupplier(() -> response.bufferFactory().wrap(resp))
        );
    }

    /**
     * 记录兜底异常的完整日志（含堆栈）。
     */
    private void writeErrorLog(ServerWebExchange exchange, Throwable ex) {
        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();
        log.error("网关未知异常 - host:{}, port:{}, uri:{}, path:{}",
                uri.getHost(), uri.getPort(), uri, request.getPath(), ex);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}