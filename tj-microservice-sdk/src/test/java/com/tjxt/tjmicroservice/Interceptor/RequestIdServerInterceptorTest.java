package com.tjxt.tjmicroservice.Interceptor;

import com.tjxt.tjcommon.Utils.RequestIdUtil;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.ServerCalls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.tjxt.tjcommon.Constants.Constant.REQUEST_ID_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * 用「真实的进程内 gRPC 服务端」验证链路追踪拦截器，而不是只做单元级模拟。
 *
 * 为什么要用真实服务端：
 *   grpc-java 会把业务回调从传输线程「跳」到应用线程执行。ThreadLocal 跟线程走，
 *   所以"在 interceptCall 里 set 一次"很可能在业务方法里读不到 —— 这个坑只有在真实
 *   Server 上（有真实的线程切换）才能暴露出来。本测试就是把它钉住：
 *   业务方法（onHalfClose 触发）所在线程必须能读到 Metadata 里的 requestId。
 */
class RequestIdServerInterceptorTest {

    private static final Metadata.Key<String> REQUEST_ID_KEY =
            Metadata.Key.of(REQUEST_ID_HEADER, Metadata.ASCII_STRING_MARSHALLER);

    /** 极简 String 编解码器，避免为了一个探针测试引入 proto 服务 */
    private static final MethodDescriptor.Marshaller<String> STRING_MARSHALLER =
            new MethodDescriptor.Marshaller<>() {
                @Override
                public InputStream stream(String value) {
                    return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
                }

                @Override
                public String parse(InputStream stream) {
                    try {
                        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        throw new IllegalStateException("解析请求失败", e);
                    }
                }
            };

    private static final MethodDescriptor<String, String> METHOD =
            MethodDescriptor.<String, String>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName("tj.test.RequestIdProbe/Call")
                    .setRequestMarshaller(STRING_MARSHALLER)
                    .setResponseMarshaller(STRING_MARSHALLER)
                    .build();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("真实 gRPC 服务端：Metadata 里的 requestId 必须出现在业务方法执行的线程上")
    void requestIdShouldReachServiceHandler() throws Exception {
        AtomicReference<String> seenInHandler = new AtomicReference<>();
        AtomicReference<String> handlerThread = new AtomicReference<>();
        AtomicReference<String> interceptThread = new AtomicReference<>();

        ServerServiceDefinition service = probeService(seenInHandler, handlerThread);

        // 额外挂一个探针拦截器，记录 interceptCall 执行时的线程，用于确认"线程跳转"确实存在
        ServerInterceptor threadProbe = new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                interceptThread.set(Thread.currentThread().getName());
                return next.startCall(call, headers);
            }
        };

        String serverName = "request-id-probe-" + System.nanoTime();
        Server server = InProcessServerBuilder.forName(serverName)
                .addService(ServerInterceptors.intercept(service, threadProbe, new RequestIdServerInterceptor()))
                .build()
                .start();
        ManagedChannel channel = InProcessChannelBuilder.forName(serverName).build();
        try {
            Metadata headers = new Metadata();
            headers.put(REQUEST_ID_KEY, "trace-grpc-1");
            Channel withHeaders = ClientInterceptors.intercept(
                    channel, MetadataUtils.newAttachHeadersInterceptor(headers));

            String response = ClientCalls.blockingUnaryCall(withHeaders, METHOD, CallOptions.DEFAULT, "ping");

            assertEquals("ok:ping", response);
            assertEquals("trace-grpc-1", seenInHandler.get(),
                    "业务方法所在线程必须能读到上游传来的 requestId");

            // 让线程信息出现在 surefire 报告里，便于确认是否真的换了线程
            System.out.println("[probe] interceptCall 线程 = " + interceptThread.get()
                    + "，业务方法线程 = " + handlerThread.get()
                    + "，是否同一线程 = " + interceptThread.get().equals(handlerThread.get()));

            // 不应污染调用方（测试）线程的 MDC
            assertNull(RequestIdUtil.get(), "不应把 requestId 留在调用方线程上");
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
            assertTrue(server.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    @DisplayName("上游没带 requestId 时兜底生成一个（服务端日志仍有链路ID）")
    void shouldGenerateWhenHeaderMissing() throws Exception {
        AtomicReference<String> seenInHandler = new AtomicReference<>();
        ServerServiceDefinition service = probeService(seenInHandler, new AtomicReference<>());

        String serverName = "request-id-probe-gen-" + System.nanoTime();
        Server server = InProcessServerBuilder.forName(serverName)
                .addService(ServerInterceptors.intercept(service, new RequestIdServerInterceptor()))
                .build()
                .start();
        ManagedChannel channel = InProcessChannelBuilder.forName(serverName).build();
        try {
            ClientCalls.blockingUnaryCall(channel, METHOD, CallOptions.DEFAULT, "ping");

            String generated = seenInHandler.get();
            assertNotNull(generated, "没有上游 requestId 时应生成一个");
            assertEquals(32, generated.length(), "应为统一的 32 位无横线 UUID，实际：" + generated);
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
            assertTrue(server.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    /** 构造一个"业务方法里读取 MDC"的探针服务 */
    private static ServerServiceDefinition probeService(
            AtomicReference<String> seenInHandler, AtomicReference<String> handlerThread) {
        return ServerServiceDefinition.builder("tj.test.RequestIdProbe")
                .addMethod(METHOD, ServerCalls.asyncUnaryCall((request, observer) -> {
                    // 这里相当于业务方法体：一元调用由 onHalfClose 触发
                    seenInHandler.set(RequestIdUtil.get());
                    handlerThread.set(Thread.currentThread().getName());
                    observer.onNext("ok:" + request);
                    observer.onCompleted();
                }))
                .build();
    }
}
