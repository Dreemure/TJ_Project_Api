package com.example.tj_project_apimicroservice.Config;

import com.example.tj_project_apicommon.Constants.Constant;
import io.grpc.*;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.GlobalClientInterceptor;

/*
 * gRPC 请求链路追踪配置。
 * 职责：为所有 gRPC 客户端请求注入 requestId 和请求来源标识（from），
 *       使下游服务能通过 MDC 拿到相同的链路追踪id。
 */
@Configuration
public class RequestIdRelayConfiguration {

    /** gRPC 中传递 requestId 的 metadata key */
    private static final Metadata.Key<String> REQUEST_ID_KEY =
            Metadata.Key.of(Constant.REQUEST_ID_HEADER, Metadata.ASCII_STRING_MARSHALLER);

    /** gRPC 中传递请求来源的 metadata key */
    private static final Metadata.Key<String> REQUEST_FROM_KEY =
            Metadata.Key.of(Constant.REQUEST_FROM_HEADER, Metadata.ASCII_STRING_MARSHALLER);

    /** gRPC 中标识调用来源的值（替代原 FEIGN_ORIGIN_NAME） */
    private static final String GRPC_ORIGIN_NAME = "grpc";

    /**
     * 全局 gRPC 客户端拦截器。
     * <p>Spring Boot gRPC Starter 会自动将它应用到所有 gRPC 客户端。
     */
    @Bean
    @Order
    @GlobalClientInterceptor
    public ClientInterceptor requestIdRelayInterceptor() {
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method,
                    CallOptions callOptions,
                    Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(
                        next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        // 1. 传递 requestId（从当前线程 MDC 取）
                        String requestId = MDC.get(Constant.REQUEST_ID_HEADER);
                        if (requestId != null) {
                            headers.put(REQUEST_ID_KEY, requestId);
                        }
                        // 2. 传递请求来源标识
                        headers.put(REQUEST_FROM_KEY, GRPC_ORIGIN_NAME);
                        super.start(responseListener, headers);
                    }
                };
            }
        };
    }
}
