package com.example.tjauth.Controller;

import com.example.tjcommon.Model.Dto.LoginUserDTO;
import com.example.tjcommon.Model.Response.R;
import com.example.tjmicroservice.Context.UserContext;      // SDK 的 UserContext
import com.example.tjauth.Utils.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/*
 * 鉴权场景示例 Controller。
 *
 * 职责：演示 5 种鉴权场景，供业务开发参考。
 * 说明：所有接口路径均为演示用，不代表真实业务。
 *
 * 五种场景：
 *   1. 公开接口：无需登录，白名单放行（SDK 的 SecurityFilterChain 配置）
 *   2. 需要登录：从 @AuthenticationPrincipal 或 UserContext 取用户
 *   3. 硬编码权限：用 @PreAuthorize 校验角色/权限点
 *   4. 动态路径权限：AuthUtils 从 Redis 拉权限规则，运营可配
 *   5. 跨服务调用：gRPC 自动透传用户信息（无需写代码）
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TestController {

    private final AuthUtils authUtils;

    // ==================== 场景 1：公开接口 ====================

    /**
     * 公开接口：无需登录。
     *
     * <p>白名单配置在 Nacos 的 tj.auth.exclude-path 中：
     * <pre>
     *   tj:
     *     auth:
     *       exclude-path:
     *         - /courses/list
     * </pre>
     *
     * <p>SDK 的 SecurityFilterChain 会放行该路径，不需要 Authorization 头。
     */
//    @GetMapping("/courses/list")
//    public R<List<CourseDTO>> list() {
//        // 无需登录，直接返回
//        return R.ok(List.of());
//    }
//
//    // ==================== 场景 2：需要登录（推荐用 @AuthenticationPrincipal） ====================
//
//    /**
//     * 需要登录才能访问。
//     *
//     * <p>原理：SDK 的 SecurityFilterChain 配置了 .anyRequest().authenticated()，
//     * 未登录请求会被拦截并返回 401。
//     *
//     * <p>@AuthenticationPrincipal 的作用：Spring Security 从 SecurityContextHolder
//     * 取出 Authentication 的 principal 字段（SDK 的 UserContextFilter 放入的 LoginUserDTO），
//     * 类型匹配后自动注入到方法参数。
//     *
//     * <p>调用链：
//     * <pre>
//     *   前端携带 Authorization: Bearer &lt;JWT&gt; 调网关
//     *     ↓
//     *   网关解析 JWT → 转成 user-info 请求头转发给本服务
//     *     ↓
//     *   SDK 的 UserContextFilter 从 user-info 解析 → 放入 SecurityContextHolder
//     *     ↓
//     *   @AuthenticationPrincipal 自动注入
//     * </pre>
//     *
//     * <p>前提：principal 类型必须是 LoginUserDTO，否则注入 null。
//     *
//     * <p>等价写法：
//     * <pre>
//     *   Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//     *   LoginUserDTO user = (LoginUserDTO) auth.getPrincipal();
//     * </pre>
//     *
//     * @param id   课程id
//     * @param user 当前登录用户（由 Spring Security 自动注入）
//     */
//    @GetMapping("/courses/{id}")
//    public R<CourseDTO> detail(@PathVariable Long id,
//                               @AuthenticationPrincipal LoginUserDTO user) {
//        log.info("查询课程，当前用户: userId={}, roleId={}, roleName={}",
//                user.getUserId(), user.getRoleId(), user.getRoleName());
//        // 业务逻辑：根据 id 查课程（此处假实现）
//        return R.ok(new CourseDTO());
//    }
//
//    // ==================== 场景 2-变体：从 UserContext 取（不依赖 Spring Security） ====================
//
//    /**
//     * 从 SDK 的 UserContext 获取当前用户（推荐，不依赖 Spring Security）。
//     *
//     * <p>UserContext 是 SDK 提供的 ThreadLocal 上下文，与 SecurityContextHolder 同时被赋值。
//     * 优点：不绑定 Spring Security，纯 gRPC 服务也能用。
//     */
//    @GetMapping("/users/me")
//    public R<LoginUserDTO> me() {
//        LoginUserDTO user = UserContext.get();
//        if (user == null) {
//            // 理论上不会到这一步（SecurityFilterChain 已保证非白名单必须登录）
//            return R.error(401, "未登录");
//        }
//        return R.ok(user);
//    }
//
//    // ==================== 场景 3：硬编码权限（@PreAuthorize） ====================
//
//    /**
//     * 硬编码：仅管理员可发布课程。
//     *
//     * <p>原理：@PreAuthorize 由 Spring Security 的 AOP 拦截器执行，
//     * 方法调用前检查当前用户的权限（从 Authentication.getAuthorities() 取）。
//     *
//     * <p>权限匹配规则：
//     *   - hasRole('ADMIN')    → 匹配权限字符串 "ROLE_ADMIN"
//     *   - hasAuthority('xxx') → 精确匹配 "xxx"（不带前缀）
//     *
//     * <p>前提：
//     *   1. 配置类上有 @EnableMethodSecurity（SDK 已加）
//     *   2. UserContextFilter 装配了 GrantedAuthority（如 "ROLE_ADMIN"）
//     *   3. JWT 里包含 roleName，网关解析后透传
//     *
//     * <p>失败行为：权限不足 → 抛 AccessDeniedException → 返回 403 JSON。
//     *
//     * <p>注意：这是"静态权限"，权限规则写死在代码里，改权限需重新部署。
//     */
//    @PreAuthorize("hasRole('ADMIN')")
//    @PostMapping("/courses")
//    public R<Void> create(@RequestBody CourseDTO dto) {
//        log.info("创建课程: {}", dto);
//        return R.ok();
//    }
//
//    /**
//     * 硬编码：教师或管理员可修改课程。
//     */
//    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
//    @PutMapping("/courses/{id}")
//    public R<Void> update(@PathVariable Long id, @RequestBody CourseDTO dto) {
//        return R.ok();
//    }
//
//    /**
//     * 硬编码：按权限点校验（推荐用法，比 hasRole 更灵活）。
//     */
//    @PreAuthorize("hasAuthority('course:delete')")
//    @DeleteMapping("/courses/{id}")
//    public R<Void> deleteByAuthority(@PathVariable Long id) {
//        return R.ok();
//    }
//
//    /**
//     * 硬编码：组合表达式——管理员或课程所属教师。
//     *
//     * <p>SpEL 表达式可访问 authentication 对象，实现更复杂的校验。
//     */
//    @PreAuthorize("hasRole('ADMIN') or #teacherId == authentication.principal.userId")
//    @DeleteMapping("/courses/{id}/teacher/{teacherId}")
//    public R<Void> deleteByTeacher(@PathVariable Long id, @PathVariable Long teacherId) {
//        return R.ok();
//    }
//
//    // ==================== 场景 4：动态路径权限（AuthUtils） ====================
//
//    /**
//     * 动态权限：由 AuthUtils 校验"路径 → 角色"。
//     *
//     * <p>原理：AuthUtils 从 Redis 读取权限规则（运营后台可配），
//     * 缓存到本地，每 20 秒自动刷新。规则形如：
//     * <pre>
//     *   {
//     *     "antPath": "/admin/**",
//     *     "roles": [1, 2]   // 允许的角色id
//     *   }
//     * </pre>
//     *
//     * <p>与 @PreAuthorize 的区别：
//     *   - @PreAuthorize：权限写死在代码，改需重新部署
//     *   - AuthUtils：权限存 DB，运营可配，动态生效
//     *
//     * <p>使用方式 A（推荐）：通过 AOP 切面自动触发，Controller 无需写代码。
//     * 见 AuthCheckAspect，拦截所有 Controller 方法自动调用 authUtils.checkAuth()。
//     *
//     * <p>使用方式 B（手动调用）：如下示例，显式传路径和用户。
//     */
//    @DeleteMapping("/admin/users/{id}")
//    public R<Void> deleteUser(@PathVariable Long id,
//                              @AuthenticationPrincipal LoginUserDTO user,
//                              HttpServletRequest request) {
//        // 手动调用动态权限校验（方式 B）
//        // 如果 AuthCheckAspect 已自动拦截，可以省略这一行
//        authUtils.checkAuth(request.getRequestURI(), user);
//
//        log.info("删除用户: id={}, 操作人: {}", id, user.getUserId());
//        return R.ok();
//    }
//
//    // ==================== 场景 5：跨服务调用（gRPC 自动透传） ====================
//
//    /**
//     * 跨服务调用示例：调用 course 服务时，SDK 自动透传用户信息。
//     *
//     * <p>原理：只要 UserContext 里有用户，SDK 的 UserRelayClientInterceptor
//     * 会在发 gRPC 请求时自动把用户信息塞到 Metadata，下游服务自动接收。
//     *
//     * <p>业务代码无需任何操作——直接注入 gRPC 客户端调用即可。
//     */
//    @GetMapping("/orders/my")
//    public R<List<Object>> myOrders(@AuthenticationPrincipal LoginUserDTO user) {
//        // 假设有 orderGrpcClient
//        // List<OrderDTO> orders = orderGrpcClient.queryByUserId(user.getUserId());
//        // ↑ 调用时，SDK 拦截器自动把 user 透传给 order-service
//        return R.ok(List.of());
//    }
//
//    // ==================== 辅助：调试当前认证信息 ====================
//
//    /**
//     * 调试接口：打印当前 Authentication 的完整信息。
//     *
//     * <p>用于排查 @PreAuthorize 不生效、401/403 问题。
//     */
//    @GetMapping("/debug/auth")
//    public R<Map<String, Object>> debugAuth(Authentication authentication) {
//        if (authentication == null) {
//            return R.error(401, "未登录");
//        }
//        return R.ok(Map.of(
//                "principal", authentication.getPrincipal(),
//                "authorities", authentication.getAuthorities(),
//                "authenticated", authentication.isAuthenticated()
//        ));
//    }
}