package com.tjxt.tjcommon.Utils;

/*
 * 用户上下文（当前登录用户ID，基于 ThreadLocal）。
 * 职责：在当前线程内保存 userId，供 MyBatis 自动填充等场景读取。
 * 说明：
 *   - 只保存 userId（不含凭据、不含权限），是最小化的用户上下文
 *   - 已注册到 ThreadContextSnapshot：@Async、虚拟线程执行器、MQ 异步发送等
 *     跨线程场景会自动携带，业务侧无需手工搬运
 * 使用（与 requestId 同一套约定：谁拥有这条线程，谁负责 set 与清理）：
 *   - 上下文边界（HTTP Filter / gRPC 服务端拦截器 / MQ 消费端拦截器）：UserContext.setUser(userId);
 *   - 边界结束（finally）：UserContext.removeUser();
 * 注意：ThreadLocal 在线程池里会复用，set 与 remove 必须成对，否则下一个请求会读到上一个用户。
 */
public class UserContext {

    private static final ThreadLocal<Long> TL = new ThreadLocal<>();

    /**
     * 上下文访问器：注册到 ThreadContextSnapshot 后，UserContext 就能跨虚拟线程/线程池传递。
     * 包级可见即可（只给 ThreadContextSnapshot 用），避免业务侧误用。
     */
    static final ThreadLocalAccessor ACCESSOR = ThreadLocalAccessor.of(TL);

    /**
     * 保存用户信息
     * @param userId 用户id
     */
    public static void setUser(Long userId){
        TL.set(userId);
    }

    /**
     * 获取用户
     * @return 用户id
     */
    public static Long getUser(){
        return TL.get();
    }

    /**
     * 移除用户信息
     */
    public static void removeUser(){
        TL.remove();
    }
}
