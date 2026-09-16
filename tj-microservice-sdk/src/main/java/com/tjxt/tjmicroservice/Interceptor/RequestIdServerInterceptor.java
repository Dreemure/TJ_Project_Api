package com.tjxt.tjmicroservice.Interceptor;

import com.tjxt.tjcommon.Utils.RequestIdUtil;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.GlobalServerInterceptor;

import static com.tjxt.tjcommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * gRPC 服务端链路追踪拦截器。
 * 职责：把请求 Metadata 里的 requestId 放进当前线程的 MDC，让 gRPC 服务端的日志也带上链路ID。
 * 说明：
 *   - 与 RequestIdRelayConfiguration（客户端拦截器）对称：客户端「MDC → Metadata」，服务端「Metadata → MDC」
 *   - 客户端拦截器早就有了，但服务端一直没有对应的接收方，导致 gRPC 线程的 MDC 为空：
 *     gRPC 服务端线程里发 MQ 消息只能凭空生成新 UUID，链路在 gRPC 这一段断掉
 *   - 与 UserRelayServerInterceptor 的区别：本拦截器传的是 requestId（链路追踪），
 *     那个传的是 user-info（登录用户），两者独立
 *
 * 【为什么在每个回调里 set 一次，而不是在 interceptCall 里 set 一次】
 *   grpc-java 会把业务回调从传输线程「跳」到应用线程执行（ServerImpl 的
 *   JumpToApplicationThreadServerStreamListener；应用侧线程池 / 虚拟线程都可能导致换线程）。
 *   interceptCall 跑在传输线程上，而真正的业务方法是在 onHalfClose（一元/服务端流）
 *   或 onMessage（客户端流/双向流）里执行的 —— 两者常常不是同一个线程。
 *   ThreadLocal 跟线程走，所以在 interceptCall 里 set 一次会被丢掉，
 *   必须在每个回调入口重新 set，并在 onComplete / onCancel 里清理，
 *   否则 requestId 会残留在复用的应用线程上（串号）。
 * 使用：由 GrpcRelayAutoConfiguration 注册为 Bean，@GlobalServerInterceptor 使其对所有 gRPC 服务端生效。
 */
@Slf4j
@GlobalServerInterceptor
public class RequestIdServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> REQUEST_ID_KEY =
            Metadata.Key.of(REQUEST_ID_HEADER, Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        // 上游没带（例如非 Java 客户端、老版本服务）就生成一个，保证服务端日志一定有链路ID
        String incoming = headers.get(REQUEST_ID_KEY);
        final String requestId = (incoming == null || incoming.isBlank())
                ? RequestIdUtil.generate()
                : incoming;

        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onReady() {
                withRequestId(requestId, super::onReady);
            }

            @Override
            public void onMessage(ReqT message) {
                withRequestId(requestId, () -> super.onMessage(message));
            }

            @Override
            public void onHalfClose() {
                withRequestId(requestId, super::onHalfClose);
            }

            @Override
            public void onCancel() {
                try {
                    super.onCancel();
                } finally {
                    RequestIdUtil.clear();
                }
            }

            @Override
            public void onComplete() {
                try {
                    super.onComplete();
                } finally {
                    RequestIdUtil.clear();
                }
            }
        };
    }

    /** 设置链路ID后执行回调（回调线程可能与 interceptCall 不同，所以每个回调都要设置） */
    private static void withRequestId(String requestId, Runnable action) {
        RequestIdUtil.mark(requestId);
        action.run();
    }
}
