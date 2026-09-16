package com.tjxt.tjmicroservice.Interceptor;

import com.alibaba.fastjson2.JSON;
import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import com.tjxt.tjmicroservice.Context.UserContext;
import io.grpc.ForwardingServerCallListener;
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
 *
 * 【为什么在每个回调里 set 一次，而不是在 interceptCall 里 set 一次】
 *   grpc-java 会把业务回调从传输线程「跳」到应用线程执行（ServerImpl 的
 *   JumpToApplicationThreadServerStreamListener，应用线程池或虚拟线程都可能换线程）。
 *   interceptCall 跑在传输线程上，而业务方法在 onHalfClose / onMessage 里执行，
 *   两者常常不是同一个线程 —— ThreadLocal 跟线程走，在 interceptCall 里 set 会被丢掉
 *   （表现为业务里 UserContext.getUser() / getUserId() 为 null）。
 *   本拦截器原来就是在 interceptCall 里设置的，存在这个隐患；现在改为每个回调入口重新 set，
 *   在同线程与换线程两种模型下都正确。
 * 注意：JSON 解析仍只在 interceptCall 里做一次，回调里只是把解析结果 set 进 ThreadLocal。
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

        // 只解析一次，回调里复用
        LoginUserDTO user = parseUser(headers);

        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onMessage(ReqT message) {
                withUser(user, () -> super.onMessage(message));
            }

            @Override
            public void onHalfClose() {
                withUser(user, super::onHalfClose);
            }

            @Override
            public void onCancel() {
                try {
                    super.onCancel();
                } finally {
                    UserContext.clear();   // ✅ 避免线程池污染
                }
            }

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

    /** 解析 user-info，失败只告警不影响调用 */
    private static LoginUserDTO parseUser(Metadata headers) {
        String json = headers.get(USER_INFO_KEY);
        if (json == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, LoginUserDTO.class);
        } catch (Exception e) {
            log.warn("解析 gRPC user-info 失败", e);
            return null;
        }
    }

    /** 设置登录用户后执行回调（回调线程可能与 interceptCall 不同，所以每个回调都要设置） */
    private static void withUser(LoginUserDTO user, Runnable action) {
        if (user != null) {
            UserContext.set(user);
        }
        action.run();
    }
}
