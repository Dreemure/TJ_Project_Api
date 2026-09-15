package com.example.tjauth.Constants;

import java.time.Duration;

/*
 * 认证常量定义。
 * 职责：集中管理 JWT payload key、Redis key 前缀、请求头名称、权限缓存 key、角色代号。
 * 使用：通过静态导入（import static ...AuthConstants.*）在认证逻辑中引用。
 * 注意：
 *   - Token 有效期（TTL）统一配置在 Nacos（tj.auth.jwt.token-ttl 等），不在此处定义
 *   - JWT 签名算法由 Auth0 java-jwt 的 Algorithm 类硬编码指定，无需常量
 *   - 角色判断建议按 roleName（如 "ADMIN"），而非 roleId，保持环境无关
 */
public final class AuthConstants {

    private AuthConstants() {
        // 常量类私有构造，防止实例化
    }

    // ==================== 角色代号 ====================
    /** 管理员的角色ID（用于数据库外键，如 role_id = 1） */
    public static final Long ADMIN_ROLE_ID = 1L;
    /** 管理员的角色代号（与数据库 role.code 一致） */
    public static final String ADMIN_ROLE_CODE = "admin";
    /** 老师的角色代号 */
    public static final String TEACHER_ROLE_CODE = "teacher";
    /** 学生的角色代号 */
    public static final String STUDENT_ROLE_CODE = "student";

    // ==================== JWT Payload Key ====================
    /** JWT payload 中存放用户信息的 key */
    public static final String PAYLOAD_USER_KEY = "user";
    /** JWT payload 中存放 JWT ID（用于防重放）的 key */
    public static final String PAYLOAD_JTI_KEY = "jti";

    // ==================== Redis Key ====================
    /** JWT 在 Redis 中的 key 前缀，完整格式：jwt:uid:{userId} */
    public static final String JWT_REDIS_KEY_PREFIX = "jwt:uid:";

    // ==================== 请求头 ====================
    /** Authorization 请求头，携带 access token */
    public static final String AUTHORIZATION_HEADER = "authorization";
    /** Refresh 请求头，携带 refresh token */
    public static final String REFRESH_HEADER = "refresh";
    /** Admin Refresh 请求头，管理端刷新 token 用 */
    public static final String ADMIN_REFRESH_HEADER = "admin-refresh";
    /** 用户信息请求头，网关解析 token 后传递用户信息 */
    public static final String USER_HEADER = "user-info";

    // ==================== 权限缓存 Key ====================
    /** 权限缓存 key */
    public static final String AUTH_PRIVILEGE_KEY = "auth:privileges";
    /** 权限缓存版本号 key（用于缓存失效） */
    public static final String AUTH_PRIVILEGE_VERSION_KEY = "version";
    /** 权限缓存刷新锁 key */
    public static final String LOCK_AUTH_PRIVILEGE_KEY = "lock:auth:privileges";
}