package com.tjxt.tjgateway.Auth;

import com.tjxt.tjcommon.Constants.ErrorInfo;
import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import com.tjxt.tjcommon.Model.Response.R;
import com.tjxt.tjgateway.Filter.JwtAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/*
 * 网关未认证入口（HTTP 401）。
 * 职责：请求未携带有效 token 时返回 R 格式的 401 JSON，而不是重定向到登录页。
 * 说明：若请求经过了 JwtAuthenticationFilter 且验签失败，会用 exchange 属性里保存的验签结果，
 *       输出精确的业务码（40101 token 已过期 / 40102 无效 token）。
 */
@Slf4j
@Component
public class JsonAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        R<?> result = exchange.getAttribute(JwtAuthenticationFilter.AUTH_RESULT_ATTRIBUTE);
        int code = result == null ? ErrorInfo.Code.UNAUTHORIZED : result.getCode();
        String msg = result == null ? ErrorInfo.Msg.UNAUTHORIZED : result.getMsg();
        log.info("网关鉴权失败：uri={}, code={}, msg={}",
                exchange.getRequest().getURI().getPath(), code, msg);
        return AuthResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, code, msg);
    }
}
