package com.tjxt.tjauth.Constants;

/*
 * 认证常量定义（auth 服务内部）。
 * 职责：集中管理 JWT 声明名、Redis key 前缀、请求头名称、权限缓存 key、角色代号。
 * 使用：通过静态导入（import static ...AuthConstants.*）在认证逻辑中引用。
 * 注意：
 *   - 与 gateway、microservice-sdk 共享的“协议常量”（请求头、JWT 声明名）统一引用
 *     com.tjxt.tjcommon.Constants.AuthConstants，避免各模块各写一份导致协议漂移
 *   - Token 有效期（TTL）统一配置在 Nacos（tj.auth.jwt.token-ttl 等），不在此处定义
 *   - JWT 签名算法由 Auth0 java-jwt 的 Algorithm 类指定（RS256），无需常量
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

    // ==================== JWT 声明（跨模块共享协议） ====================
    /** 用户ID */
    public static final String CLAIM_USER_ID = com.tjxt.tjcommon.Constants.AuthConstants.CLAIM_USER_ID;
    /** 角色ID */
    public static final String CLAIM_ROLE_ID = com.tjxt.tjcommon.Constants.AuthConstants.CLAIM_ROLE_ID;
    /** 角色代号 */
    public static final String CLAIM_ROLE_NAME = com.tjxt.tjcommon.Constants.AuthConstants.CLAIM_ROLE_NAME;
    /** 用户类型：1-员工 2-学员 3-老师 */
    public static final String CLAIM_TYPE = com.tjxt.tjcommon.Constants.AuthConstants.CLAIM_TYPE;
    /** token 类型标记（access / refresh） */
    public static final String CLAIM_TOKEN_TYPE = com.tjxt.tjcommon.Constants.AuthConstants.CLAIM_TOKEN_TYPE;
    /** token 类型：access */
    public static final String TOKEN_TYPE_ACCESS = com.tjxt.tjcommon.Constants.AuthConstants.TOKEN_TYPE_ACCESS;
    /** token 类型：refresh */
    public static final String TOKEN_TYPE_REFRESH = com.tjxt.tjcommon.Constants.AuthConstants.TOKEN_TYPE_REFRESH;

    /** JWT payload 中存放用户信息的 key（历史字段，新 token 使用独立声明） */
    public static final String PAYLOAD_USER_KEY = "user";
    /** JWT payload 中存放 JWT ID（用于防重放）的 key */
    public static final String PAYLOAD_JTI_KEY = "jti";

    // ==================== Redis Key ====================
    /** JWT 在 Redis 中的 key 前缀，完整格式：jwt:uid:{jti} */
    public static final String JWT_REDIS_KEY_PREFIX = "jwt:uid:";
    /** 权限缓存 key（与网关共享的协议常量，见 common 的 AuthConstants） */
    public static final String AUTH_PRIVILEGE_KEY = com.tjxt.tjcommon.Constants.AuthConstants.AUTH_PRIVILEGE_KEY;
    /** 权限缓存版本号 key（网关据此判断是否需要刷新本地缓存） */
    public static final String AUTH_PRIVILEGE_VERSION_KEY = com.tjxt.tjcommon.Constants.AuthConstants.AUTH_PRIVILEGE_VERSION_KEY;
    /** 权限缓存刷新锁 key */
    public static final String LOCK_AUTH_PRIVILEGE_KEY = "lock:auth:privileges";

    // ==================== 请求头（跨模块共享协议） ====================
    /** Authorization 请求头，携带 access token */
    public static final String AUTHORIZATION_HEADER = com.tjxt.tjcommon.Constants.AuthConstants.AUTHORIZATION_HEADER;
    /** Refresh 请求头/cookie，携带 refresh token（前台） */
    public static final String REFRESH_HEADER = com.tjxt.tjcommon.Constants.AuthConstants.REFRESH_HEADER;
    /** Admin Refresh 请求头/cookie，管理端刷新 token 用 */
    public static final String ADMIN_REFRESH_HEADER = com.tjxt.tjcommon.Constants.AuthConstants.ADMIN_REFRESH_HEADER;
    /** 用户信息请求头，网关解析 token 后注入（Base64(JSON)） */
    public static final String USER_HEADER = com.tjxt.tjcommon.Constants.AuthConstants.USER_HEADER;
}
