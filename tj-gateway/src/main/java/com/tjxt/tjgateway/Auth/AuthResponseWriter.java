package com.tjxt.tjgateway.Auth;

import com.tjxt.tjcommon.Constants.Constant;
import com.tjxt.tjcommon.Model.Response.R;
import com.tjxt.tjcommon.Utils.JsonUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/*
 * 网关鉴权响应写出工具。
 * 职责：把鉴权失败结果按项目统一格式（R&lt;T&gt; JSON）写回客户端。
 * 说明：供 ServerAuthenticationEntryPoint（401）与 ServerAccessDeniedHandler（403）共用，
 *       保证网关直接返回的错误与业务服务的错误格式完全一致。
 */
public final class AuthResponseWriter {

    private AuthResponseWriter() {
        // 工具类私有构造，防止实例化
    }

    /**
     * 写出 R 格式的 JSON 错误响应。
     *
     * @param exchange ServerWebExchange
     * @param status   HTTP 状态码
     * @param code     业务码
     * @param msg      提示信息
     * @return 响应写出结果
     */
    public static Mono<Void> write(ServerWebExchange exchange, HttpStatus status, int code, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(new IllegalStateException("响应已提交，无法写出鉴权错误信息"));
        }
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        R<Void> body = R.error(code, msg);
        // requestId 从请求头取（网关入口 RequestIdRelayFilter 注入）
        String requestId = exchange.getRequest().getHeaders().getFirst(Constant.REQUEST_ID_HEADER);
        if (requestId != null) {
            body.requestId(requestId);
        }
        byte[] bytes = JsonUtils.toJsonStr(body).getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.fromSupplier(() -> response.bufferFactory().wrap(bytes)));
    }
}
