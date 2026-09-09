package com.example.tj_project_apicommon.Filters;

import com.example.tj_project_apicommon.Constants.Constant;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

/*
 * 请求ID过滤器。
 * 职责：从 HTTP 请求头中提取请求ID（REQUEST_ID_HEADER），存入 MDC，供链路追踪日志使用。
 * 行为：在请求开始时从请求头取值并放入 MDC，请求结束后清除 MDC。
 * 使用：通过 @WebFilter 自动注册，需配合 @ServletComponentScan 或通过 @Component 显式注册。
 * 注意：使用 @Order(HIGHEST_PRECEDENCE) 确保最先执行，保证后续日志能获取到 traceId。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@WebFilter(filterName = "requestIdFilter", urlPatterns = "/**")
public class RequestIdFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 1.获取request
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        // 2.获取请求头中的requestId
        String requestId = request.getHeader(Constant.REQUEST_ID_HEADER);
        try {
            // 3.存入MDC
            MDC.put(Constant.REQUEST_ID_HEADER, requestId);
            filterChain.doFilter(request, servletResponse);
        }finally {
            // 4.移除
            MDC.clear();
        }
    }
}
