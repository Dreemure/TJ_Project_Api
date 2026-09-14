package com.example.tjmicroservice.Filter;

import com.alibaba.fastjson2.JSON;
import com.example.tjcommon.Model.Dto.LoginUserDTO;
import com.example.tjmicroservice.Context.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/*
 * 用户上下文过滤器。
 * 职责：从网关传递的 user-info 请求头解析用户信息，放入 Spring Security 上下文和 SDK 的 UserContext。
 * 说明：由 UserContextAutoConfiguration 自动注册，业务服务无需手动配置。
 */
@Slf4j
public class UserContextFilter extends OncePerRequestFilter {

    private static final String USER_INFO_HEADER = "user-info";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        try {
            String userInfoJson = request.getHeader(USER_INFO_HEADER);
            if (StringUtils.hasText(userInfoJson)) {
                LoginUserDTO user = JSON.parseObject(userInfoJson, LoginUserDTO.class);

                // 1. 放入 Spring Security 上下文（供 @PreAuthorize 使用）
                var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);

                // 2. 放入 UserContext（供 gRPC 拦截器透传）
                UserContext.set(user);
            }
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
