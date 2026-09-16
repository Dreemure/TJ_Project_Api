package com.tjxt.tjcommon.Utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/*
 * RequestIdUtil 单元测试：锁定「格式统一」与「写 / 不写 MDC 的边界」。
 * 这几点一旦被改回旧实现（java.util.UUID 带横线、MDC.clear() 清空全部、put null），测试必须失败。
 */
class RequestIdUtilTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("generate：32 位无横线 UUID（与网关 RequestIdRelayFilter 格式一致）")
    void generateShouldReturnDashlessUuid() {
        String id = RequestIdUtil.generate();
        assertEquals(32, id.length(), "应为 32 位无横线 UUID，实际：" + id);
        assertFalse(id.contains("-"), "不应带横线，实际：" + id);
        assertNotEquals(id, RequestIdUtil.generate(), "两次生成不应相同");
    }

    @Test
    @DisplayName("getOrCreate：MDC 为空时生成新值，但不写入 MDC（不污染调用方线程）")
    void getOrCreateShouldNotTouchMdc() {
        assertNull(RequestIdUtil.get());
        assertNotNull(RequestIdUtil.getOrCreate());
        assertNull(RequestIdUtil.get(), "getOrCreate 只应返回值，不应写入 MDC");
    }

    @Test
    @DisplayName("getOrCreate：MDC 已有值时原样返回")
    void getOrCreateShouldReuseExisting() {
        RequestIdUtil.mark("trace-1");
        assertEquals("trace-1", RequestIdUtil.getOrCreate());
    }

    @Test
    @DisplayName("markRequest：非 Web 线程下也能兜底生成，且幂等不覆盖")
    void markRequestShouldGenerateAndBeIdempotent() {
        String first = RequestIdUtil.markRequest();
        assertNotNull(first);
        assertEquals(first, RequestIdUtil.markRequest(), "MDC 已有值不应被覆盖");
        assertEquals(first, RequestIdUtil.get());
    }

    @Test
    @DisplayName("mark：null / 空白不覆盖已有值")
    void markShouldIgnoreBlank() {
        RequestIdUtil.mark("trace-1");
        RequestIdUtil.mark(null);
        RequestIdUtil.mark("   ");
        assertEquals("trace-1", RequestIdUtil.get());
    }

    @Test
    @DisplayName("clear：只清 requestId，不影响 MDC 中其他键")
    void clearShouldOnlyRemoveRequestId() {
        MDC.put("tenant", "t1");
        RequestIdUtil.mark("trace-1");
        RequestIdUtil.clear();
        assertNull(RequestIdUtil.get());
        assertEquals("t1", MDC.get("tenant"), "clear 不应清空整个 MDC");
    }

    @Test
    @DisplayName("wrap：把父线程 MDC 搬进新线程，且任务结束后清理干净（不串号）")
    void wrapShouldPropagateAndCleanUp() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            RequestIdUtil.mark("trace-wrap");

            // 线程池会复用线程：第一个任务应读到父线程的 id
            CompletableFuture<String> first = new CompletableFuture<>();
            pool.execute(MarkedRunnable.wrap(() -> first.complete(RequestIdUtil.get())));
            assertEquals("trace-wrap", first.get(5, TimeUnit.SECONDS));

            // 父线程清空后再提交第二个任务：
            // 同一个池化线程必须读不到上一次的值 —— 这证明 MarkedRunnable 的 finally 清理生效了
            RequestIdUtil.clear();
            CompletableFuture<Object> second = new CompletableFuture<>();
            pool.execute(MarkedRunnable.wrap(() -> second.complete(RequestIdUtil.get())));
            assertNull(second.get(5, TimeUnit.SECONDS), "上一条任务的 requestId 不应残留在复用的线程里");
        } finally {
            pool.shutdownNow();
        }
    }
}
