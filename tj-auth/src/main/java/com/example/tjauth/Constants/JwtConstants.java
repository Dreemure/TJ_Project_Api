package com.example.tjauth.Constants;

import java.time.Duration;

/*
 * JWT 认证常量定义。
 * 职责：集中管理 JWT 相关的 payload key、Redis key 前缀、过期时间、请求头名称、权限缓存 key 等。
 * 使用：通过静态导入（import static ...JwtConstants.*）在认证逻辑中引用。
 * 注意：
 *   - JWT_TOKEN_TTL 为 access token 有效期，生产环境建议 5 分钟；
 *   - JWT_REFRESH_TTL 为 refresh token 有效期，用于换取新的 access token；
 *   - JWT_REMEMBER_ME_TTL 为"记住我"场景下的 token 有效期；
 *   - 所有 Redis key 统一加业务前缀，避免与其他业务冲突。
 */
public final class JwtConstants {

    private JwtConstants() {
        // 常量类私有构造，防止实例化
    }

    // ==================== JWT Payload Key ====================
    /** JWT payload 中存放用户信息的 key */
    public static final String PAYLOAD_USER_KEY = "user";
    /** JWT payload 中存放 JWT ID（用于防重放）的 key */
    public static final String PAYLOAD_JTI_KEY = "jti";

    // ==================== Redis Key ====================
    /** JWT 在 Redis 中的 key 前缀，完整格式：jwt:uid:{userId} */
    public static final String JWT_REDIS_KEY_PREFIX = "jwt:uid:";

    // ==================== Token 过期时间 ====================
    /**
     * access token 有效期。
     * <p>生产环境建议 5 分钟；测试期间可临时调大（如 1 天）便于调试。
     */
    public static final Duration JWT_TOKEN_TTL = Duration.ofMinutes(5);

    /** refresh token 有效期，用于换取新的 access token */
    public static final Duration JWT_REFRESH_TTL = Duration.ofMinutes(30);

    /** "记住我"场景下的 token 有效期 */
    public static final Duration JWT_REMEMBER_ME_TTL = Duration.ofDays(7);

    // ==================== 算法与请求头 ====================
    /** JWT 签名算法：RS256（RSA + SHA-256） */
    public static final String JWT_ALGORITHM = "rs256";

    /** Authorization 请求头名称，携带 access token */
    public static final String AUTHORIZATION_HEADER = "authorization";

    /** Refresh 请求头名称，携带 refresh token */
    public static final String REFRESH_HEADER = "refresh";

    /** Admin Refresh 请求头名称，管理端刷新 token 用 */
    public static final String ADMIN_REFRESH_HEADER = "admin-refresh";

    /** 用户信息请求头名称，网关解析 token 后传递用户信息 */
    public static final String USER_HEADER = "user-info";

    // ==================== 权限缓存 Key ====================
    /** 权限缓存 key */
    public static final String AUTH_PRIVILEGE_KEY = "auth:privileges";

    /** 权限缓存版本号 key（用于缓存失效） */
    public static final String AUTH_PRIVILEGE_VERSION_KEY = "version";

    /** 权限缓存刷新锁 key */
    public static final String LOCK_AUTH_PRIVILEGE_KEY = "lock:auth:privileges";
}


