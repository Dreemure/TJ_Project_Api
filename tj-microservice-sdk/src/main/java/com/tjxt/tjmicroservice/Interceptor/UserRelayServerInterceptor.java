package com.tjxt.tjmicroservice.Interceptor;

import com.alibaba.fastjson2.JSON;
import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import com.tjxt.tjmicroservice.Context.UserContext;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.GlobalServerInterceptor;

/*
 * gRPC 服务端用户信息接收拦截器。
 * 职责：从 Metadata 取出 user-info，放入 UserContext。
 * 说明：请求结束后清理 ThreadLocal，防止线程复用污染。
 *       @GlobalServerInterceptor 由 Spring gRPC 识别，注册为 Bean 后对全部 gRPC 服务端生效
 *       （注册见 GrpcRelayAutoConfiguration）。
 */
@Slf4j
@GlobalServerInterceptor
public class UserRelayServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> USER_INFO_KEY =
            Metadata.Key.of("user-info", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String json = headers.get(USER_INFO_KEY);
        if (json != null) {
            try {
                LoginUserDTO user = JSON.parseObject(json, LoginUserDTO.class);
                UserContext.set(user);
            } catch (Exception e) {
                log.warn("解析 gRPC user-info 失败", e);
            }
        }

        // 包装 listener，请求结束时清理
        return new SimpleForwardingServerCallListener<>(next.startCall(call, headers)) {
            @Override
            public void onComplete() {
                try {
                    super.onComplete();
                } finally {
                    UserContext.clear();   // ✅ 避免线程池污染
                }
            }
        };
    }

    /** 简化的 listener 包装类 */
    private static abstract class SimpleForwardingServerCallListener<ReqT>
            extends io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
        public SimpleForwardingServerCallListener(ServerCall.Listener<ReqT> delegate) {
            super(delegate);
        }
    }
}
