package com.tjxt.tjauth.Filter;

import com.tjxt.tjauth.Constants.AuthConstants;
import com.tjxt.tjauth.Utils.AuthUtils;
import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import com.tjxt.tjcommon.Model.Response.R;
import com.tjxt.tjcommon.Utils.RoleUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/*
 * 认证服务 JWT 认证过滤器。
 * 职责：从 authorization 请求头解析 access token，验签成功后把 LoginUserDTO 写入 SecurityContext，
 *       供后续授权（SecurityFilterChain）、方法级注解（@PreAuthorize）与动态权限切面（AuthCheckAspect）使用。
 * 说明：
 *   - 支持裸 token 与 "Bearer xxx" 两种写法
 *   - 验签失败不在这里抛异常：只把结果挂在 request 属性上，交给 SecurityConfig 的认证入口统一输出 401 JSON
 *     （这样白名单路径与未登录访问的报错口径完全一致）
 *   - 该类不是 Spring Bean（由 SecurityConfig 显式 new 出来并挂进过滤器链），避免被 Servlet 容器重复注册
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 验签结果在 request 上的属性名：供认证入口输出精确业务码 */
    public static final String AUTH_RESULT_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".AUTH_RESULT";

    private final AuthUtils authUtils;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            R<LoginUserDTO> result = authUtils.parseToken(token);
            if (result.success()) {
                LoginUserDTO user = result.getData();
                List<GrantedAuthority> authorities = RoleUtils.resolve(user).stream()
                        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                        .toList();
                var authentication = new UsernamePasswordAuthenticationToken(user, token, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                if (log.isDebugEnabled()) {
                    log.debug("auth 服务认证通过：userId={}, roleId={}, roles={}",
                            user.getUserId(), user.getRoleId(), authorities);
                }
            } else {
                // 记录失败原因（40101 已过期 / 40102 无效），由认证入口输出
                request.setAttribute(AUTH_RESULT_ATTRIBUTE, result);
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 从请求头解析 token：兼容 "Bearer xxx" 与裸 token。
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(header)) {
            return null;
        }
        String token = header.trim();
        String bearerPrefix = com.tjxt.tjcommon.Constants.AuthConstants.BEARER_PREFIX;
        if (token.regionMatches(true, 0, bearerPrefix, 0, bearerPrefix.length())) {
            token = token.substring(bearerPrefix.length()).trim();
        }
        return StringUtils.hasText(token) ? token : null;
    }
}
