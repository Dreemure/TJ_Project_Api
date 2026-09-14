package com.example.tjauth.Utils;

import com.alibaba.fastjson2.JSON;
import cn.hutool.core.text.AntPathMatcher;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.tjauth.Config.AuthProperties;
import com.example.tjauth.Model.Dto.PrivilegeRoleDTO;
import com.example.tjcommon.Exceptions.ForbiddenException;
import com.example.tjcommon.Exceptions.UnauthorizedException;
import com.example.tjcommon.Model.Dto.LoginUserDTO;
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

import static com.example.tjauth.Constants.AuthErrorInfo.Code.INVALID_TOKEN_CODE;
import static com.example.tjauth.Constants.AuthErrorInfo.Msg.*;
import static com.example.tjauth.Constants.JwtConstants.*;

/*
 * 认证与权限校验工具（与 Spring Security 互补）。
 *
 * 职责：
 *   1. 解析 JWT，返回登录用户信息（RS256 + 公钥验签）
 *   2. 基于"路径 → 角色"的权限模型，校验当前用户是否有权访问
 *   3. 定时从 Redis 拉取权限数据，缓存到本地
 *
 * 与 Spring Security 的关系：
 *   - Spring Security 负责：禁用默认登录页、CSRF、Session 管理、白名单放行
 *   - 本类负责：动态路径级权限（权限数据存 DB，运营可配，无需重新部署）
 *   - 两者协作：Security 放行 → AuthUtil 校验路径权限 → 业务执行
 *
 * 说明：
 *   - JWT 由 auth 服务的 JwtIssuer 用私钥签发，本类用公钥验签
 *   - 权限数据来源于 auth 库，由业务写入 Redis Hash
 *   - 本类不适合网关层使用（网关已排除 Redis 依赖），仅在 auth 服务内生效
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthUtils {

    /** 权限缓存：antPath → 权限规则 */
    private Map<String, PrivilegeRoleDTO> privileges = new HashMap<>();
    /** 需拦截的路径匹配符集合 */
    private Set<String> paths = new HashSet<>();
    /** 本地缓存的权限版本号 */
    private volatile int privilegeVersion = -1;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final AuthProperties authProperties;
    private final RsaKeyLoader rsaKeyLoader;
    private final StringRedisTemplate stringRedisTemplate;

    private BoundHashOperations<String, String, String> hashOps;

    /** JWT 验签器（用公钥构造，线程安全，可复用） */
    private JWTVerifier jwtVerifier;

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
        log.info("AuthUtil 初始化完成，JWT 验签算法：RS256");
    }

    // ==================== 1. JWT 解析 ====================

    /**
     * 解析 JWT，返回登录用户信息。
     *
     * @param token JWT 字符串（不含 Bearer 前缀）
     * @return LoginUserDTO；解析失败返回 null
     */
    public LoginUserDTO parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            DecodedJWT jwt = jwtVerifier.verify(token);
            Long userId = jwt.getClaim("userId").asLong();
            Long roleId = jwt.getClaim("roleId").asLong();
            if (userId == null) {
                return null;
            }
            var user = new LoginUserDTO();
            user.setUserId(userId);
            user.setRoleId(roleId);
            return user;
        } catch (TokenExpiredException e) {
            log.debug("JWT 已过期: {}", e.getMessage());
            return null;
        } catch (JWTVerificationException e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
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
        String matchPath = findMatchPath(antPath);
        if (matchPath == null) {
            return;   // 未配置权限，放行
        }
        if (user == null || user.getUserId() == null) {
            throw new UnauthorizedException(INVALID_TOKEN_CODE, UNAUTHORIZED);
        }
        PrivilegeRoleDTO pathPrivilege = privileges.get(matchPath);
        if (pathPrivilege == null || CollectionUtils.isEmpty(pathPrivilege.getRoles())) {
            return;
        }
        if (!pathPrivilege.getRoles().contains(user.getRoleId())) {
            throw new ForbiddenException(FORBIDDEN);
        }
    }

    private String findMatchPath(String antPath) {
        for (String pattern : paths) {
            if (antPathMatcher.match(pattern, antPath)) {
                return pattern;
            }
        }
        return null;
    }

    // ==================== 3. 定时刷新权限 ====================

    @Scheduled(fixedDelay = 20_000)
    public void refreshTask() {
        int currentVersion = currentVersion();
        if (currentVersion == privilegeVersion) {
            return;
        }

        List<PrivilegeRoleDTO> privilegeRoleDTOS = loadPrivileges();
        Map<String, PrivilegeRoleDTO> map = privilegeRoleDTOS.stream()
                .collect(Collectors.toMap(PrivilegeRoleDTO::getAntPath, p -> p, (a, b) -> a));

        this.privileges = map;
        this.paths = map.keySet();
        this.privilegeVersion = currentVersion;
        log.info("权限缓存已刷新，版本：{}，共 {} 条", currentVersion, map.size());
    }

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