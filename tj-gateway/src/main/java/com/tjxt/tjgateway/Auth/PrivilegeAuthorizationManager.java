package com.tjxt.tjgateway.Auth;

import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import com.tjxt.tjgateway.Config.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/*
 * 网关鉴权管理器（Spring Security 的 ReactiveAuthorizationManager）。
 * 职责：对"非白名单"请求做两件事（顺序固定）：
 *   1. 认证：必须已登录（匿名/未认证一律拒绝，由认证入口输出 401）
 *   2. 鉴权：若权限表（auth:privileges）为该"方法+路径"配置了角色，则要求当前用户的 roleId 在允许集合内，
 *      否则由无权限处理器输出 403；未配置权限的路径登录即可访问
 * 说明：
 *   - 参照老项目 AccountAuthFilter + AuthUtil#checkAuth 的语义，但改为 Spring Security 的授权组件实现
 *   - 权限数据由 auth 服务的 PrivilegeService 维护（PrivilegeController -> Redis auth:privileges + version）
 *   - 细粒度（数据级）鉴权仍由业务服务的 @PreAuthorize 承担：网关提供 ROLE_xxx 与 user-info 透传
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrivilegeAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final PrivilegeCache privilegeCache;
    private final AuthProperties authProperties;

    /**
     * 授权判定：先要求已认证，再按权限表校验角色。
     * <p>注：Spring Security 7 中 {@code authorize} 是 ReactiveAuthorizationManager 的主方法
     * （{@code check} 已标记为过时并默认委托给本方法）。
     *
     * @param authentication 当前认证信息（可能为匿名）
     * @param context        授权上下文（拿到 ServerWebExchange）
     * @return 授权结果：true 放行，false 触发 401（未登录）或 403（已登录但无权限）
     */
    @Override
    public Mono<AuthorizationResult> authorize(Mono<Authentication> authentication, AuthorizationContext context) {
        ServerHttpRequest request = context.getExchange().getRequest();
        String path = request.getPath().value();

        // 1. 命中的权限规则（未配置则为 null，表示登录即可访问）
        PrivilegeRule rule = authProperties.getPrivilege().isEnabled()
                ? privilegeCache.findRule(request.getMethod(),
                List.of(path, PrivilegeRuleMatcher.stripFirstSegment(path)))
                : null;

        // 2. 认证 + 鉴权
        return authentication
                .filter(PrivilegeAuthorizationManager::isAuthenticated)
                .map(auth -> decide(auth, rule, request))
                // 没有任何认证信息（等价于匿名）：拒绝，交给认证入口返回 401
                .defaultIfEmpty(new AuthorizationDecision(false));
    }

    /**
     * 已登录用户是否允许访问。
     */
    private AuthorizationResult decide(Authentication authentication, PrivilegeRule rule, ServerHttpRequest request) {
        if (rule == null) {
            return new AuthorizationDecision(true);
        }
        Long roleId = roleId(authentication);
        boolean allowed = rule.allows(roleId);
        if (!allowed) {
            log.info("网关鉴权拒绝：method={}, path={}, 规则={}, 当前角色={}",
                    request.getMethod(), request.getPath().value(), rule.antPath(), roleId);
        }
        return new AuthorizationDecision(allowed);
    }

    /**
     * 是否已认证（匿名身份不算）。
     */
    private static boolean isAuthenticated(Authentication authentication) {
        return authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
    }

    /**
     * 取当前登录用户的角色ID（网关认证过滤器写入的 LoginUserDTO）。
     */
    private Long roleId(Authentication authentication) {
        return authentication.getPrincipal() instanceof LoginUserDTO user ? user.getRoleId() : null;
    }
}
