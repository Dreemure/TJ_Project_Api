package com.tjxt.tjcommon.Autoconfigure.VirtualThread;

import com.tjxt.tjcommon.Utils.RequestIdUtil;
import com.tjxt.tjcommon.Utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * 回归测试：虚拟线程 + 线程上下文（MDC / UserContext）传递。
 *
 * 背景（本模块最容易出错的地方）：
 *   MDC 与 UserContext 底层都是普通 ThreadLocal（不是 InheritableThreadLocal），
 *   虚拟线程不会继承父线程的上下文。所以「父线程有 requestId，子线程就有」是错的 ——
 *   必须靠 ThreadContextExecutor / ThreadContextTaskDecorator 显式搬运。
 *   一旦有人把这层包装拆掉（例如直接返回裸执行器），下面的用例必须立刻失败。
 */
class VirtualThreadConfigTest {

    private static final String REQUEST_ID = "trace-46b8ca1f";
    private static final Long USER_ID = 10086L;

    private final VirtualThreadConfig config = new VirtualThreadConfig();

    @AfterEach
    void tearDown() {
        MDC.clear();
        UserContext.removeUser();
    }

    /** 在目标线程里把「上下文 + 是否虚拟线程」采集出来 */
    private static CompletableFuture<Object[]> probe(Executor executor) {
        CompletableFuture<Object[]> captured = new CompletableFuture<>();
        executor.execute(() -> captured.complete(new Object[]{
                RequestIdUtil.get(),
                UserContext.getUser(),
                Thread.currentThread().isVirtual()
        }));
        return captured;
    }

    @Test
    @DisplayName("virtualThreadExecutor：虚拟线程 + requestId + userId 全部传递")
    void virtualThreadExecutorShouldPropagateContext() throws Exception {
        Executor executor = config.virtualThreadExecutor();
        RequestIdUtil.mark(REQUEST_ID);
        UserContext.setUser(USER_ID);
        try {
            Object[] result = probe(executor).get(5, TimeUnit.SECONDS);
            assertEquals(REQUEST_ID, result[0], "虚拟线程里必须能读到父线程的 requestId");
            assertEquals(USER_ID, result[1], "虚拟线程里必须能读到父线程的 userId");
            assertEquals(Boolean.TRUE, result[2], "任务应运行在虚拟线程上");
        } finally {
            closeQuietly(executor);
        }
    }

    @Test
    @DisplayName("@Async 执行器：虚拟线程 + requestId + userId 全部传递")
    void asyncExecutorShouldPropagateContext() throws Exception {
        AsyncTaskExecutor executor = config.tjFallbackApplicationTaskExecutor(config.threadContextTaskDecorator());
        RequestIdUtil.mark(REQUEST_ID);
        UserContext.setUser(USER_ID);
        try {
            Object[] result = probe(executor).get(5, TimeUnit.SECONDS);
            assertEquals(REQUEST_ID, result[0], "@Async 执行器必须传递 requestId");
            assertEquals(USER_ID, result[1], "@Async 执行器必须传递 userId（UserContext）");
            assertEquals(Boolean.TRUE, result[2], "@Async 应使用虚拟线程");
        } finally {
            closeQuietly(executor);
        }
    }

    @Test
    @DisplayName("父线程没有上下文时，子线程也不应凭空出现值（隔离性）")
    void shouldNotLeakBetweenTasks() throws Exception {
        Executor executor = config.virtualThreadExecutor();
        MDC.clear();
        UserContext.removeUser();
        try {
            Object[] result = probe(executor).get(5, TimeUnit.SECONDS);
            assertNull(result[0], "空的 requestId 上下文应保持为空");
            assertNull(result[1], "空的 UserContext 应保持为空");
        } finally {
            closeQuietly(executor);
        }
    }

    @Test
    @DisplayName("复用线程（平台线程池）上不残留上一次的上下文")
    void shouldClearContextOnReusedThread() throws Exception {
        // 用平台线程池模拟"线程会被复用"的场景：这是虚拟线程不会遇到、但线程池一定会遇到的问题
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Executor executor = new ThreadContextExecutor(pool);
        try {
            RequestIdUtil.mark(REQUEST_ID);
            UserContext.setUser(USER_ID);
            Object[] first = probe(executor).get(5, TimeUnit.SECONDS);
            assertEquals(REQUEST_ID, first[0]);
            assertEquals(USER_ID, first[1]);

            // 父线程清空后再提交：同一个池化线程必须读不到上一次的值
            MDC.clear();
            UserContext.removeUser();
            Object[] second = probe(executor).get(5, TimeUnit.SECONDS);
            assertNull(second[0], "上一次的 requestId 不应残留在复用的线程里");
            assertNull(second[1], "上一次的 userId 不应残留在复用的线程里");
        } finally {
            closeQuietly(executor);
        }
    }

    /** 执行器可能不是 AutoCloseable（视 Spring 版本而定），这里安全关闭 */
    private static void closeQuietly(Object executor) {
        if (executor instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                throw new AssertionError("关闭执行器失败", e);
            }
        } else {
            assertTrue(false, "执行器应可关闭，避免线程泄漏");
        }
    }
}
