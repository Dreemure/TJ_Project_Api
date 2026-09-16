package com.tjxt.tjmicroservice.Context;

import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import com.tjxt.tjcommon.Utils.ThreadContextSnapshot;
import com.tjxt.tjcommon.Utils.ThreadLocalAccessor;

/*
 * 用户上下文（基于 ThreadLocal）。
 * 职责：在当前线程内保存登录用户信息，供透传拦截器读取。
 * 说明：替代 Spring Security 的 SecurityContextHolder，避免 SDK 强依赖 Security。
 * 使用（谁拥有这条线程，谁负责 set 与清理）：
 *   - 上下文边界（HTTP 的 UserContextFilter / gRPC 的 UserRelayServerInterceptor）：UserContext.set(user);
 *   - 边界结束：UserContext.clear();
 * 注意：已注册到 ThreadContextSnapshot，因此 @Async、虚拟线程执行器、MQ 异步发送等跨线程场景
 *      会自动携带登录用户（虚拟线程不继承父线程 ThreadLocal，不注册就会丢）。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUserDTO> HOLDER = new ThreadLocal<>();

    static {
        // 注册到上下文快照机制：跨线程（虚拟线程/线程池/MQ 异步）自动传递登录用户
        ThreadContextSnapshot.register(ThreadLocalAccessor.of(HOLDER));
    }

    private UserContext() {}

    public static void set(LoginUserDTO user) {
        HOLDER.set(user);
    }

    public static LoginUserDTO get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUserDTO user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
