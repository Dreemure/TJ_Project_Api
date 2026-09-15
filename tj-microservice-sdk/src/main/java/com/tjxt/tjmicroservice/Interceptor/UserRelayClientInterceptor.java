package com.tjxt.tjmicroservice.Interceptor;

import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import com.tjxt.tjcommon.Utils.JsonUtils;
import com.tjxt.tjmicroservice.Context.UserContext;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.springframework.grpc.client.GlobalClientInterceptor;

/*
 * gRPC 用户信息透传拦截器。
 * 职责：调用 gRPC 服务时，把 UserContext 中的用户信息放到 Metadata 传递给下游。
 * 说明：@GlobalClientInterceptor 由 Spring gRPC 识别，注册为 Bean 后对全部 gRPC 客户端生效
 *       （注册见 GrpcRelayAutoConfiguration）。
 */
@Slf4j
@GlobalClientInterceptor
public class UserRelayClientInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> USER_INFO_KEY =
            Metadata.Key.of("user-info", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // ✅ 从 UserContext 取用户信息（不再用 SecurityContextHolder）
                LoginUserDTO user = UserContext.get();
                if (user != null) {
                    try {
                        headers.put(USER_INFO_KEY, JsonUtils.toJsonStr(user));
                    } catch (Exception e) {
                        log.warn("序列化 user-info 失败", e);
                    }
                }
                super.start(responseListener, headers);
            }
        };
    }
}
