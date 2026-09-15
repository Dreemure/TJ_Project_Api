package com.tjxt.tjcommon.Constants;

/*
 * 认证协议常量（跨模块共享）。
 * 职责：作为 auth 服务（签发 token）、gateway（验签 + 透传用户信息）、microservice-sdk（消费用户信息）
 *       三方共同遵守的“线上协议”，避免各模块各自定义字符串导致协议漂移。
 * 使用：静态导入（import static com.tjxt.tjcommon.Constants.AuthConstants.*）或直接引用。
 * 注意：
 *   - 只放跨模块共享的协议常量；auth 服务内部的业务常量（角色代号、权限缓存 key 等）放模块自己的常量类
 *   - token 有效期、issuer 等可配置项走 tj.auth.jwt.* 配置，不在此处定义
 */
public final class AuthConstants {

    private AuthConstants() {
        // 常量类私有构造，防止实例化
    }

    // ==================== 请求头 ====================
    /** 携带 access token 的请求头 */
    public static final String AUTHORIZATION_HEADER = "authorization";
    /** Bearer 前缀：兼容 "Bearer xxx" 写法；本项目前端直接放裸 token，两种都支持 */
    public static final String BEARER_PREFIX = "Bearer ";
    /** 携带 refresh token 的 cookie/请求头（前台） */
    public static final String REFRESH_HEADER = "refresh";
    /** 携带 refresh token 的 cookie/请求头（管理端） */
    public static final String ADMIN_REFRESH_HEADER = "admin-refresh";
    /** 网关验签后注入、下游服务消费的用户信息头，值为 Base64(JSON(LoginUserDTO)) */
    public static final String USER_HEADER = "user-info";

    // ==================== JWT 声明 ====================
    /** 用户ID */
    public static final String CLAIM_USER_ID = "userId";
    /** 角色ID */
    public static final String CLAIM_ROLE_ID = "roleId";
    /** 角色代号（如 admin / teacher / student） */
    public static final String CLAIM_ROLE_NAME = "roleName";
    /** 用户类型：1-员工 2-学员 3-老师（对应 LoginUserDTO.type 与 UserType 枚举） */
    public static final String CLAIM_TYPE = "type";
    /** token 类型标记，值为 access / refresh */
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    /** token 类型：access token */
    public static final String TOKEN_TYPE_ACCESS = "access";
    /** token 类型：refresh token */
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    // ==================== 角色名 ====================
    /** Spring Security 角色前缀 */
    public static final String ROLE_PREFIX = "ROLE_";
    /** 员工（管理端）角色 */
    public static final String ROLE_STAFF = "ROLE_STAFF";
    /** 学员（前台）角色 */
    public static final String ROLE_STUDENT = "ROLE_STUDENT";
    /** 老师角色 */
    public static final String ROLE_TEACHER = "ROLE_TEACHER";

    // ==================== 权限表（auth 服务写入 / 网关读取） ====================
    /** 权限表 Redis Hash：field = "METHOD:/uri"，value = PrivilegeRoleDTO 的 JSON */
    public static final String AUTH_PRIVILEGE_KEY = "auth:privileges";
    /** 权限表版本号 key：auth 服务每次修改权限后 +1，网关/profile 消费者据此判断是否需要刷新本地缓存 */
    public static final String AUTH_PRIVILEGE_VERSION_KEY = "version";

    // ==================== auth 服务 ====================
    /** auth 服务名（服务发现用） */
    public static final String AUTH_SERVICE_NAME = "auth-service";
    /** auth 服务暴露 JWK 公钥的路径 */
    public static final String JWKS_PATH = "/jwks";
    /** 默认 issuer，需与 auth 服务 tj.auth.jwt.issuer 保持一致 */
    public static final String DEFAULT_ISSUER = "tj-auth";
}
