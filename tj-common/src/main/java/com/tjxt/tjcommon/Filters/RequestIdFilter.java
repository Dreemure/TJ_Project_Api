package com.tjxt.tjcommon.Filters;

import com.tjxt.tjcommon.Constants.Constant;
import com.tjxt.tjcommon.Utils.RequestIdUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/*
 * 请求ID过滤器。
 * 职责：保证每个 HTTP 请求线程的 MDC 里一定有 requestId，供日志链路追踪与 R 响应体使用。
 * 行为：
 *   1. 请求头带了 requestId（网关 RequestIdRelayFilter 注入）就用它，保证跨服务是同一条链路
 *   2. 请求头没带（直连服务、内部调用、压测）则生成一个，避免 MDC 为空导致日志与响应体没有链路ID
 *   3. 请求结束后只清理 requestId，不清空整个 MDC，避免误伤其他 MDC 键
 * 使用：通过 MvcConfig 的 FilterRegistrationBean 注册；不要在类上标注 @WebFilter/@Order（会重复注册且顺序失效）。
 * 注意：
 *   - 必须最高优先级，保证后续所有日志都能取到 requestId
 *   - Tomcat 线程池会复用线程，所以必须在 finally 里清理，否则下一个请求会读到上一个请求的 id（串号）
 *   - 若开启了 spring.threads.virtual.enabled，每个请求一个虚拟线程，天然不会串号，但清理逻辑仍应保留
 */
public class RequestIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest request)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        // 1. 请求头优先：上游（网关/其他服务）透传下来的链路ID
        String requestId = request.getHeader(Constant.REQUEST_ID_HEADER);
        // 2. 兜底：没带就生成，保证 MDC 一定有值（原实现在这里是 MDC.put(key, null)）
        if (requestId == null || requestId.isBlank()) {
            requestId = RequestIdUtil.generate();
        }
        RequestIdUtil.mark(requestId);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            // 3. 只清 requestId（原实现用 MDC.clear()，会顺手清掉线程里其他 MDC 键）
            RequestIdUtil.clear();
        }
    }
}
