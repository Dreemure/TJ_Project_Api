package com.tjxt.tjmicroservice.AutoConfiguration;

import com.tjxt.tjmicroservice.Interceptor.RequestIdServerInterceptor;
import com.tjxt.tjmicroservice.Interceptor.UserRelayClientInterceptor;
import com.tjxt.tjmicroservice.Interceptor.UserRelayServerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/*
 * gRPC 用户信息透传自动配置。
 * 职责：把"用户信息透传"拦截器注册为全局 gRPC 拦截器（客户端 + 服务端各一个），
 *      并注册服务端链路追踪拦截器（requestId → MDC）。
 * 说明：
 *   - 客户端拦截器：把当前线程 UserContext 里的登录用户放进 gRPC Metadata（明文 JSON 的 user-info）
 *   - 服务端拦截器：从 Metadata 取出用户信息放入 UserContext，请求结束清理（防止线程池污染）
 *   - 服务端链路拦截器：从 Metadata 取出 requestId 放入 MDC，请求结束清理
 *     （客户端发送 requestId 见 RequestIdRelayConfiguration；以前只有发送方没有接收方，
 *      导致 gRPC 线程 MDC 为空、链路在 gRPC 这一段断掉）
 *   - 拦截器类上分别标注了 @GlobalClientInterceptor / @GlobalServerInterceptor，
 *     Spring gRPC 会把它们应用到所有 gRPC 客户端/服务端
 *   - 用 @ConditionalOnClass 守卫：未引入对应 gRPC starter 的服务自动跳过
 * 使用：业务服务无需任何配置，服务间调用会自动携带登录用户信息与链路追踪ID。
 */
@AutoConfiguration
public class GrpcRelayAutoConfiguration {

    /**
     * 客户端透传拦截器：调用下游 gRPC 服务时携带当前登录用户。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.grpc.client.GlobalClientInterceptor")
    public UserRelayClientInterceptor userRelayClientInterceptor() {
        return new UserRelayClientInterceptor();
    }

    /**
     * 服务端接收拦截器：把上游传来的用户信息放进 UserContext。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.grpc.server.GlobalServerInterceptor")
    public UserRelayServerInterceptor userRelayServerInterceptor() {
        return new UserRelayServerInterceptor();
    }

    /**
     * 服务端链路拦截器：把上游传来的 requestId 放进 MDC（日志链路追踪）。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.grpc.server.GlobalServerInterceptor")
    public RequestIdServerInterceptor requestIdServerInterceptor() {
        return new RequestIdServerInterceptor();
    }
}
