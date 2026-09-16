package com.tjxt.tjmicroservice.Context;

import com.tjxt.tjcommon.Autoconfigure.VirtualThread.ThreadContextExecutor;
import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/*
 * SDK 用户上下文的跨线程传递回归测试。
 *
 * 背景：UserContext 是普通 ThreadLocal，虚拟线程不继承父线程上下文，
 * 所以 gRPC / HTTP 那些上下文边界设置的用户，在 @Async 方法、虚拟线程任务里默认是读不到的
 * （表现为 UserContext.getUserId() 为 null，gRPC 透传下游时会丢用户）。
 * SDK 的 UserContext 在静态代码块里把自己注册进了 tj-common 的 ThreadContextSnapshot，
 * 本用例锁定这个注册行为：一旦有人把 static 块删掉，测试必须失败。
 */
class UserContextPropagationTest {

    private static final Long USER_ID = 10086L;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("UserContext 能传进虚拟线程，且任务结束后在复用线程上不残留")
    void shouldPropagateAcrossThreadsAndCleanUp() throws Exception {
        // 用单线程平台池模拟"线程会被复用"，这样既能验证传递，也能验证清理
        ExecutorService pool = Executors.newSingleThreadExecutor();
        ThreadContextExecutor executor = new ThreadContextExecutor(pool);
        try {
            UserContext.set(loginUser());
            CompletableFuture<Long> first = new CompletableFuture<>();
            executor.execute(() -> first.complete(UserContext.getUserId()));
            assertEquals(USER_ID, first.get(5, TimeUnit.SECONDS), "虚拟线程/子线程里必须能读到登录用户");

            // 父线程清空后再提交：同一个池化线程必须读不到上一次的用户
            UserContext.clear();
            CompletableFuture<Long> second = new CompletableFuture<>();
            executor.execute(() -> second.complete(UserContext.getUserId()));
            assertNull(second.get(5, TimeUnit.SECONDS), "上一个任务的用户不应残留在复用的线程里");
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("虚拟线程同样能拿到 UserContext")
    void shouldPropagateToVirtualThread() throws Exception {
        ExecutorService virtualThreads = Executors.newVirtualThreadPerTaskExecutor();
        ThreadContextExecutor executor = new ThreadContextExecutor(virtualThreads);
        try {
            UserContext.set(loginUser());
            CompletableFuture<Object[]> captured = new CompletableFuture<>();
            executor.execute(() -> captured.complete(new Object[]{
                    UserContext.getUserId(),
                    Thread.currentThread().isVirtual()
            }));
            Object[] result = captured.get(5, TimeUnit.SECONDS);
            assertEquals(USER_ID, result[0]);
            assertEquals(Boolean.TRUE, result[1]);
        } finally {
            executor.close();
        }
    }

    private static LoginUserDTO loginUser() {
        LoginUserDTO user = new LoginUserDTO();
        user.setUserId(USER_ID);
        user.setType(2);
        return user;
    }
}
