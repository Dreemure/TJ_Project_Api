package com.tjxt.tjmicroservice.Filter;

import com.alibaba.fastjson2.JSON;
import com.tjxt.tjcommon.Constants.AuthConstants;
import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import com.tjxt.tjcommon.Utils.RoleUtils;
import com.tjxt.tjmicroservice.Context.UserContext;
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
 *   1. Spring Security 上下文（供 @PreAuthorize / 过滤器链授权使用）
 *   2. UserContext（供 gRPC 拦截器透传）
 * 说明：
 *   - 由 UserContextAutoConfiguration 自动注册，业务服务无需手动配置
 *   - 头部格式为 Base64(JSON(LoginUserDTO))，由网关 UserInfoRelayFilter 注入
 *   - 角色口径与网关、auth 服务一致：用户类型 → ROLE_STAFF/ROLE_STUDENT/ROLE_TEACHER，
 *     角色代号（roleName）→ ROLE_<CODE>，统一由 common 的 RoleUtils 解析，便于后续细化鉴权
 *   - 请求结束后清理 ThreadLocal，防止线程池污染
 */
@Slf4j
public class UserContextFilter extends OncePerRequestFilter {

    private static final String USER_INFO_HEADER = AuthConstants.USER_HEADER;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        try {
            String userInfoRaw = request.getHeader(USER_INFO_HEADER);
            if (StringUtils.hasText(userInfoRaw)) {
                String json = new String(Base64.getDecoder().decode(userInfoRaw), StandardCharsets.UTF_8);
                LoginUserDTO user = JSON.parseObject(json, LoginUserDTO.class);

                // 1. 映射为 Spring Security 角色名（与网关、auth 服务同一口径）
                List<GrantedAuthority> authorities = RoleUtils.resolve(user).stream()
                        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                        .toList();

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
}