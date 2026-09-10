package com.example.tj_project_apicommon.Utils;

import cn.hutool.core.lang.UUID;
import org.slf4j.MDC;

import static com.example.tj_project_apicommon.Constants.Constant.REQUEST_ID_HEADER;

/*
 * 请求ID工具类。
 * 职责：确保当前线程的 MDC 中存在 REQUEST_ID_HEADER，来源优先级：MDC 已有 > 请求头 > 自动生成 UUID。
 * 使用：Web 入口（Filter/Interceptor）或异步任务开始时调用 markRequest()，结束时调用 clear()。
 * 注意：clear() 会清空整个 MDC，若线程中还有其他 MDC 数据，需谨慎使用。
 */
public class RequestIdUtil {
    private RequestIdUtil() {
        // 工具类私有构造
    }

    /**
     * 标记当前线程的请求ID。
     * <p>若 MDC 中已存在 REQUEST_ID_HEADER，则直接返回；
     * 否则依次尝试从请求头获取，仍未获取则生成 UUID。
     */
    public static void markRequest() {
        // 1. 已存在则跳过
        if (MDC.get(REQUEST_ID_HEADER) != null) {
            return;
        }
        // 2. 尝试从请求头获取
        var requestId = WebUtils.getRequestId();
        // 3. 兜底：生成 UUID
        if (requestId == null) {
            requestId = UUID.randomUUID().toString(true);
        }
        // 4. 存入 MDC
        MDC.put(REQUEST_ID_HEADER, requestId);
    }

    /**
     * 清理当前线程的 MDC。
     * <p>建议在请求结束或异步任务完成的 finally 块中调用。
     */
    public static void clear() {
        MDC.clear();
    }
}
