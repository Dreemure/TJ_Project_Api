package com.tjxt.tjauth.Utils;

import cn.hutool.core.text.AntPathMatcher;
import com.alibaba.fastjson2.JSON;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.tjxt.tjauth.Config.AuthProperties;
import com.tjxt.tjauth.Model.Dto.PrivilegeRoleDTO;
import com.tjxt.tjcommon.Exceptions.ForbiddenException;
import com.tjxt.tjcommon.Exceptions.UnauthorizedException;
import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import com.tjxt.tjcommon.Model.Response.R;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.stream.Collectors;

import static com.tjxt.tjauth.Constants.AuthConstants.*;
import static com.tjxt.tjauth.Constants.AuthErrorInfo.Code.EXPIRED_TOKEN_CODE;
import static com.tjxt.tjauth.Constants.AuthErrorInfo.Code.INVALID_TOKEN_CODE;
import static com.tjxt.tjauth.Constants.AuthErrorInfo.Msg.*;

/*
 * 认证与权限校验工具（与 Spring Security 互补）。
 *
 * ==================== 职责 ====================
 *   1. 解析 JWT，返回登录用户信息（RS256 + 公钥验签）
 *   2. 基于"路径 → 角色"的权限模型，校验当前用户是否有权访问
 *   3. 定时从 Redis 拉取权限数据，缓存到本地
 *
 * ==================== 与 Spring Security 的关系 ====================
 *   - Spring Security 负责：禁用默认登录页、CSRF、Session 管理、白名单放行
 *   - 本类负责：动态路径级权限（权限数据存 DB，运营可配，无需重新部署）
 *   - 两者协作：Security 放行 → AuthUtils 校验路径权限 → 业务执行
 *
 * ==================== 说明 ====================
 *   - JWT 由 auth 服务的 JwtIssuer 用私钥签发，本类用公钥验签
 *   - 权限数据来源于 auth 库，由业务写入 Redis Hash
 *   - 本类不适合网关层使用（网关已排除 Redis 依赖），仅在 auth 服务内生效
 *   - 启动时通过 @PostConstruct 同步加载一次权限缓存，保证服务就绪前数据已加载
 *   - 之后由 @Scheduled 定时刷新（默认 20 秒一次）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthUtils {

    /*
     * volatile = "任何线程改了，其他线程立刻看到"，保证多线程之间的可见性。
     */

    /** 权限缓存：antPath → 权限规则 */
    private volatile Map<String, PrivilegeRoleDTO> privileges = new HashMap<>();
    /** 需拦截的路径匹配符集合 */
    private volatile Set<String> paths = new HashSet<>();
    /** 本地缓存的权限版本号（初始 -1，保证首次一定刷新） */
    private volatile int privilegeVersion = -1;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final AuthProperties authProperties;
    private final RsaKeyLoader rsaKeyLoader;
    private final StringRedisTemplate stringRedisTemplate;

    private BoundHashOperations<String, String, String> hashOps;

    /** JWT 验签器（用公钥构造，线程安全，可复用） */
    private JWTVerifier jwtVerifier;

    // ==================== 初始化 ====================

    /**
     * 启动初始化。
     * <p>1. 初始化 Redis 权限 Hash 操作
     * <p>2. 加载公钥，构造 JWT 验签器（RS256）
     * <p>3. 同步加载一次权限缓存（保证服务对外可用前已就绪）
     */
    @PostConstruct
    public void init() {
        // 1. 初始化 Redis 权限 Hash 操作
        this.hashOps = stringRedisTemplate.boundHashOps(AUTH_PRIVILEGE_KEY);

        // 2. 加载公钥并构造 JWT 验签器（RS256）
        PublicKey publicKey = rsaKeyLoader.loadPublicKey(
                authProperties.getJwt().getPublicKeyPath());
        this.jwtVerifier = JWT.require(
                        Algorithm.RSA256((RSAPublicKey) publicKey, null))
                .withIssuer(authProperties.getJwt().getIssuer())
                .build();

        // 3. 同步加载一次权限缓存（失败不阻塞启动，交给 @Scheduled 重试）
        try {
            refreshTask();
        } catch (Exception e) {
            log.error("启动加载权限缓存失败，将在 20 秒后重试", e);
        }

        log.info("AuthUtils 初始化完成，JWT 验签算法：RS256");
    }

    // ==================== 1. JWT 解析 ====================

    /**
     * 解析 JWT，返回登录用户信息。
     *
     * @param token JWT 字符串（不含 Bearer 前缀）
     * @return R&lt;LoginUserDTO&gt;：成功为 ok(user)，失败带 40101（过期）/40102（无效）业务码
     */
    public R<LoginUserDTO> parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            return R.error(INVALID_TOKEN_CODE, INVALID_TOKEN);
        }
        try {
            DecodedJWT jwt = jwtVerifier.verify(token);
            // 0. refresh token 不能当 access token 用（只能走 /accounts/refresh 换取新 token）
            String tokenType = jwt.getClaim(CLAIM_TOKEN_TYPE).asString();
            if (StringUtils.hasText(tokenType) && !TOKEN_TYPE_ACCESS.equals(tokenType)) {
                log.debug("token 类型不是 access: {}", tokenType);
                return R.error(INVALID_TOKEN_CODE, INVALID_TOKEN);
            }
            Long userId = jwt.getClaim(CLAIM_USER_ID).asLong();
            if (userId == null) {
                return R.error(INVALID_TOKEN_CODE, INVALID_TOKEN_PAYLOAD);
            }
            var user = new LoginUserDTO();
            user.setUserId(userId);
            user.setRoleId(jwt.getClaim(CLAIM_ROLE_ID).asLong());
            user.setRoleName(jwt.getClaim(CLAIM_ROLE_NAME).asString());
            user.setType(readType(jwt));
            return R.ok(user);
        } catch (TokenExpiredException e) {
            log.debug("JWT 已过期: {}", e.getMessage());
            return R.error(EXPIRED_TOKEN_CODE, EXPIRED_TOKEN);
        } catch (JWTVerificationException e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            return R.error(INVALID_TOKEN_CODE, INVALID_TOKEN);
        }
    }

    /**
     * 读取用户类型声明（1-员工 2-学员 3-老师）。
     * <p>声明缺失或格式非法时返回 null（仅导致下游无角色权限，不影响登录态）。
     */
    private Integer readType(DecodedJWT jwt) {
        try {
            return jwt.getClaim(CLAIM_TYPE).asInt();
        } catch (Exception e) {
            log.debug("token 中 {} 声明格式非法: {}", CLAIM_TYPE, e.getMessage());
            return null;
        }
    }

    // ==================== 2. 路径权限校验 ====================

    /**
     * 校验当前用户是否有权访问指定路径。
     *
     * @param antPath 请求路径（如 /users/123）
     * @param user    登录用户信息；为 null 表示未登录
     * @throws UnauthorizedException 未登录
     * @throws ForbiddenException    无权限
     */
    public void checkAuth(String antPath, LoginUserDTO user) {
        // 1. 路径是否配置了权限
        String matchPath = findMatchPath(antPath);
        if (matchPath == null) {
            return;   // 未配置权限，放行
        }
        // 2. 是否登录
        if (user == null || user.getUserId() == null) {
            throw new UnauthorizedException(INVALID_TOKEN_CODE, UNAUTHORIZED);
        }
        // 3. 角色校验
        PrivilegeRoleDTO pathPrivilege = privileges.get(matchPath);
        if (pathPrivilege == null || CollectionUtils.isEmpty(pathPrivilege.getRoles())) {
            return;
        }
        if (!pathPrivilege.getRoles().contains(user.getRoleId())) {
            throw new ForbiddenException(FORBIDDEN);
        }
    }

    /**
     * 匹配请求路径与已配置的权限路径。
     *
     * @param antPath 请求路径
     * @return 匹配的权限路径模式；无匹配返回 null
     */
    private String findMatchPath(String antPath) {
        for (String pattern : paths) {
            if (antPathMatcher.match(pattern, antPath)) {
                return pattern;
            }
        }
        return null;
    }

    // ==================== 3. 定时刷新权限 ====================

    /**
     * 定时刷新权限缓存。
     * <p>每 20 秒执行一次，先比对版本号，版本未变则跳过（减少 Redis 读取）。
     * <p>启动时由 {@link #init()} 主动调用一次，保证服务就绪前缓存已加载。
     */
    @Scheduled(fixedDelay = 20_000)
    public void refreshTask() {
        // 1. 读取 Redis 中的版本号
        int currentVersion = currentVersion();
        if (currentVersion == privilegeVersion) {
            return;   // 版本未变，无需刷新
        }

        // 2. 从 Redis 加载权限数据
        List<PrivilegeRoleDTO> privilegeRoleDTOS = loadPrivileges();
        Map<String, PrivilegeRoleDTO> map = privilegeRoleDTOS.stream()
                .collect(Collectors.toMap(PrivilegeRoleDTO::getAntPath, p -> p, (a, b) -> a));

        // 3. 原子替换（先构造新集合，再赋值，避免读到半成品）
        this.privileges = map;
        this.paths = map.keySet();
        this.privilegeVersion = currentVersion;
        log.info("权限缓存已刷新，版本：{}，共 {} 条", currentVersion, map.size());
    }

    /**
     * 从 Redis Hash 加载权限数据。
     *
     * @return 权限规则列表；无数据返回空列表
     */
    private List<PrivilegeRoleDTO> loadPrivileges() {
        List<String> values = hashOps.values();
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(json -> {
                    try {
                        return JSON.parseObject(json, PrivilegeRoleDTO.class);
                    } catch (Exception e) {
                        log.warn("权限数据解析失败: {}", json, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 读取 Redis 中的权限版本号。
     *
     * @return 版本号；读取失败或未设置返回 0
     */
    private int currentVersion() {
        String version = stringRedisTemplate.opsForValue().get(AUTH_PRIVILEGE_VERSION_KEY);
        if (!StringUtils.hasText(version)) {
            return 0;
        }
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}