package com.tjxt.tjgateway.Filter;

import cn.hutool.core.lang.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static com.tjxt.tjcommon.Constants.Constant.GATEWAY_ORIGIN_NAME;
import static com.tjxt.tjcommon.Constants.Constant.REQUEST_FROM_HEADER;
import static com.tjxt.tjcommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * 请求ID中继过滤器（网关入口，最先执行）。
 * 职责：
 *   1. 生成 requestId，写入 MDC（供日志链路追踪）与请求头（供下游服务透传）
 *   2. 标记请求来源为 gateway（下游可用 WebUtils#isGatewayRequest 判断）
 * 说明：网关自己生成 requestId 而不是沿用客户端传入值，避免外部伪造链路ID；请求结束后清理 MDC（线程池复用）。
 */
@Slf4j
@Component
public class RequestIdRelayFilter implements WebFilter, Ordered {

    /** 支付回调等第三方通知接口不标记网关来源 */
    private static final String NOTIFY_PATH_PREFIX = "/ps/notify";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 1. 生成 requestId 并写入日志变量池
        String requestId = UUID.randomUUID().toString(true);
        MDC.put(REQUEST_ID_HEADER, requestId);

        // 2. 更新请求头：requestId + 来源标识
        String path = exchange.getRequest().getPath().value();
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set(REQUEST_ID_HEADER, requestId);
                    if (!path.startsWith(NOTIFY_PATH_PREFIX)) { // 如果是非网关内资源则不标记去处
                        headers.set(REQUEST_FROM_HEADER, GATEWAY_ORIGIN_NAME); // 标记要转到哪个服务
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signal -> MDC.remove(REQUEST_ID_HEADER)); // 返回后移除MDC日志清空线程
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
