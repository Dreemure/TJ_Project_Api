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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/*
 * 用户上下文过滤器。
 * 职责：从网关传递的 user-info 请求头解析用户信息，放入：
 *   1. Spring Security 上下文（供 @PreAuthorize 使用）
 *   2. UserContext（供 gRPC 拦截器透传）
 * 说明：
 *   - 由 UserContextAutoConfiguration 自动注册，业务服务无需手动配置
 *   - 身份鉴权采用单角色模型：user.type 直接映射为 ROLE_STAFF / ROLE_STUDENT / ROLE_TEACHER
 *   - 不依赖 role / privilege 表，后续需要细粒度权限时再扩展此处
 *   - 请求结束后清理 ThreadLocal，防止线程池污染
 */
@Slf4j
public class UserContextFilter extends OncePerRequestFilter {

    private static final String USER_INFO_HEADER = "user-info";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        try {
            String userInfoRaw = request.getHeader(USER_INFO_HEADER);
            if (StringUtils.hasText(userInfoRaw)) {
                String json = new String(Base64.getDecoder().decode(userInfoRaw), StandardCharsets.UTF_8);
                LoginUserDTO user = JSON.parseObject(json, LoginUserDTO.class);

                // 1. 根据 user.type 映射为 Spring Security 角色
                List<GrantedAuthority> authorities = resolveAuthorities(user);

                // 2. 放入 Spring Security 上下文
                var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                // 3. 放入 UserContext（供 gRPC 拦截器透传）
                UserContext.set(user);
            }
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 将 user.type 映射为 Spring Security 角色。
     * 1-员工 / 2-学员 / 3-老师，未知类型不授予任何身份。
     */
    private List<GrantedAuthority> resolveAuthorities(LoginUserDTO user) {
        if (user.getType() == null) {
            return List.of();
        }
        String role = switch (user.getType()) {
            case 1 -> "ROLE_STAFF";
            case 2 -> "ROLE_STUDENT";
            case 3 -> "ROLE_TEACHER";
            default -> null;
        };
        return role == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority(role));
    }
}