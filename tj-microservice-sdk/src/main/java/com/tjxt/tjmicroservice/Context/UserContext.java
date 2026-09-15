package com.tjxt.tjmicroservice.Context;

import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;

/*
 * 用户上下文（基于 ThreadLocal）。
 * 职责：在当前线程内保存登录用户信息，供透传拦截器读取。
 * 说明：替代 Spring Security 的 SecurityContextHolder，避免 SDK 强依赖 Security。
 * 使用：
 *   - 服务入口处：UserContext.set(user);
 *   - 请求结束后：UserContext.clear();
 */
public final class UserContext {

    private static final ThreadLocal<LoginUserDTO> HOLDER = new ThreadLocal<>();

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
