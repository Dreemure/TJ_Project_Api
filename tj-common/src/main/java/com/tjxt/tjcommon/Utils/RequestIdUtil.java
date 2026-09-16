package com.tjxt.tjcommon.Utils;

import cn.hutool.core.lang.UUID;
import org.slf4j.MDC;

import java.util.concurrent.Callable;

import static com.tjxt.tjcommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * 请求ID（链路追踪ID）工具类 —— 全项目 requestId 的唯一来源（读取 / 生成 / 跨线程传递都走这里）。
 *
 * 职责：
 *   1. 读取 / 生成 requestId（格式统一：32 位无横线 UUID，与网关 RequestIdRelayFilter 一致）
 *   2. 保证当前线程 MDC 中存在 requestId（markRequest / mark）
 *   3. 跨线程传递上下文（wrap）—— 虚拟线程不会继承父线程的 MDC，必须显式传递
 *
 * 【MDC 与虚拟线程的三个事实，决定了这套工具的设计】
 *   1. MDC 底层是普通 ThreadLocal（不是 InheritableThreadLocal）：
 *      任何新建线程（包括虚拟线程）里 MDC 都是空的，"父线程有子线程就有"是错的。
 *   2. 虚拟线程不池化、不继承：不存在"上一条任务残留"的串号问题，
 *      但每一次异步跳转（@Async、CompletableFuture、MQ 异步发送）都会丢失 MDC。
 *   3. 平台线程池（Tomcat、Rabbit 消费线程）会复用线程：
 *      必须在 finally 里 clear()，否则下一条任务会读到上一个请求的 requestId（串号）。
 *
 * 使用约定（谁写 MDC、谁清理，取决于"谁拥有这条线程"）：
 *   上下文边界（HTTP 的 RequestIdFilter / MQ 消费端拦截器 / gRPC 服务端拦截器 / 定时任务包装）
 *     → markRequest() 或 mark(id)：在这里"重置"当前线程的上下文。所谓边界，指的是
 *       拥有该线程完整生命周期、因而有资格写、也有时机清理的那段代码；
 *       "数据的起点"（发送端/生产者）不算边界，它只做「导出」（写消息头，见 TraceIdChannelInterceptor）
 *   边界结束（请求 / 任务的 finally）
 *     → clear()：写 MDC 的人负责清理，否则平台线程池复用线程时会串号
 *   跨线程跳转（@Async / CompletableFuture / 线程池）
 *     → wrap(task)，或注入 virtualThreadExecutor（已内建传递）
 */
public final class RequestIdUtil {

    private RequestIdUtil() {
        // 工具类私有构造
    }

    /**
     * 读取当前线程 MDC 中的 requestId。
     *
     * @return requestId；当前线程没有则返回 null
     */
    public static String get() {
        return MDC.get(REQUEST_ID_HEADER);
    }

    /**
     * 生成一个新的 requestId（32 位无横线 UUID）。
     * <p>全项目只有这一处生成 requestId，保证日志、响应体、消息头里的格式完全一致。
     */
    public static String generate() {
        return UUID.fastUUID().toString(true);
    }

    /**
     * 取当前线程的 requestId，取不到就生成一个。
     * <p><b>不会写入 MDC</b>：调用方只是想要一个"写进消息头/响应体"的值，
     * 不应顺带污染当前线程的上下文。需要写 MDC 请用 {@link #mark(String)} 或 {@link #markRequest()}。
     *
     * @return 一定非 null 的 requestId
     */
    public static String getOrCreate() {
        String requestId = get();
        return requestId != null ? requestId : generate();
    }

    /**
     * 确保当前线程的 MDC 中存在 requestId，并返回最终生效的值。
     * <p>取值优先级：MDC 已有 &gt; HTTP 请求头 &gt; 新生成 UUID。
     * <p>幂等：MDC 里已有值时直接返回，不会覆盖（避免打断已经建立的链路）。
     * <p>非 Web 线程（定时任务、MQ 消费线程）调用也安全：请求头取不到时会生成 UUID。
     *
     * @return 最终写入 MDC 的 requestId
     */
    public static String markRequest() {
        String requestId = get();
        if (requestId != null) {
            return requestId;
        }
        // 非 Web 线程下 RequestContextHolder 取不到请求，WebUtils 返回 null，属于预期情况
        String fromHeader = WebUtils.getRequestId();
        requestId = (fromHeader != null && !fromHeader.isBlank()) ? fromHeader : generate();
        MDC.put(REQUEST_ID_HEADER, requestId);
        return requestId;
    }

    /**
     * 把指定 requestId 写入当前线程 MDC（消息头 → MDC 的消费端场景）。
     * <p>入参为 null 或空白字符串时不覆盖，避免把已有的有效链路ID清掉。
     *
     * @param requestId 上游传下来的 requestId
     */
    public static void mark(String requestId) {
        if (requestId != null && !requestId.isBlank()) {
            MDC.put(REQUEST_ID_HEADER, requestId);
        }
    }

    /**
     * 清理当前线程的 requestId。
     * <p>只 remove REQUEST_ID_HEADER，<b>不清空整个 MDC</b>：
     * 若线程里还有其他 MDC 数据（租户、业务 traceId 等），不会被误伤。
     */
    public static void clear() {
        MDC.remove(REQUEST_ID_HEADER);
    }

    /**
     * 包装 Runnable，让它在目标线程里带上当前线程的上下文（MDC + 已注册的 UserContext 等）。
     *
     * @param runnable 原始任务
     * @return 带上下文传递的任务
     */
    public static Runnable wrap(Runnable runnable) {
        return MarkedRunnable.wrap(runnable);
    }

    /**
     * 包装 Callable，让它在目标线程里带上当前线程的上下文（配合 CompletableFuture.supplyAsync）。
     *
     * @param callable 原始任务
     * @param <T>      返回值类型
     * @return 带上下文传递的任务
     */
    public static <T> Callable<T> wrap(Callable<T> callable) {
        return MarkedCallable.wrap(callable);
    }
}
