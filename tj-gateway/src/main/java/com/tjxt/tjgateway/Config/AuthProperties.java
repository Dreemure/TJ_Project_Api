package com.tjxt.tjgateway.Config;

import com.tjxt.tjcommon.Constants.AuthConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/*
 * 网关认证鉴权配置。
 * 职责：绑定 tj.auth.* 配置，提供 SecurityWebFilterChain（白名单）、JWT 验签与路径级鉴权所需参数。
 * 说明：
 *   - 白名单 = 代码兜底（DEFAULT_EXCLUDE_PATH）+ 配置项（exclude-path），只增不减
 *     （老项目 tj-gateway 的 AuthProperties#afterPropertiesSet 也是代码内置默认白名单）
 *   - exclude-path 支持两种写法：纯路径（ant 风格，如 /auth/accounts/login）与 "METHOD:路径"（如 POST:/auth/accounts/login）
 *   - JWT 公钥默认通过服务发现从 auth 服务的 /jwks 拉取，也可用 public-key-path 指定本地 PEM
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tj.auth")
public class AuthProperties {

    /*
     * 代码级兜底白名单（与配置取并集，只增不减）。
     * 目的：某环境 Nacos 配置未同步时，"必须公开"的登录/公钥/健康检查路径不会因为漏配而 401。
     * 注意：路径按项目路由约定书写（/auth/** + StripPrefix=1，见 docker/nacos/gateway-service.yaml），
     *       路由前缀不同请以 Nacos 的 exclude-path 为准（兜底项不会妨碍额外配置）。
     */
    public static final Set<String> DEFAULT_EXCLUDE_PATH = Set.of(
            "/error/**",
            "/actuator/**",
            "/favicon.ico",
            "/auth/jwks",
            "/auth/accounts/login",
            "/auth/accounts/admin/login",
            "/auth/accounts/refresh",
            "/auth/accounts/admin/refresh",
            "/auth/accounts/logout"
    );

    /** 免认证路径白名单，由 yml 配置。 */
    private Set<String> excludePath = new LinkedHashSet<>();

    /**
     * 最终白名单 = 代码兜底 + 配置项（并集）。
     * <p>与 SDK 的 AutoAuthProperties#getFinalExcludePath 风格保持一致。
     */
    public Set<String> getFinalExcludePath() {
        Set<String> result = new LinkedHashSet<>(DEFAULT_EXCLUDE_PATH);
        if (excludePath != null) {
            result.addAll(excludePath);
        }
        return result;
    }

    /** JWT 验签配置。 */
    private Jwt jwt = new Jwt();

    /** JWK 公钥加载配置。 */
    private Jwks jwks = new Jwks();

    /** 路径级鉴权配置（权限表 auth:privileges）。 */
    private Privilege privilege = new Privilege();

    @Getter
    @Setter
    public static class Privilege {

        /**
         * 是否开启路径级鉴权。
         * <p>true：非白名单请求除认证外，还要按权限表校验"方法+路径 → 角色"；
         * false：只做认证（白名单 + 登录校验），角色校验交给下游服务。
         */
        private boolean enabled = true;

        /** 权限缓存刷新间隔（先比对版本号，版本未变不重复读取数据）。 */
        private Duration refreshInterval = Duration.ofSeconds(20);

        /** 读取 Redis 的超时时间（超时/失败时保留上一次快照）。 */
        private Duration timeout = Duration.ofSeconds(3);
    }

    @Getter
    @Setter
    public static class Jwt {

        /**
         * 期望的 issuer；留空表示不校验 issuer。
         * <p>推荐与 auth 服务的 tj.auth.jwt.issuer 配置保持一致后填写，可防止其它系统签发的 token 混入。
         */
        private String issuer;

        /**
         * 可选：本地公钥 PEM 位置（file: 或 classpath: 前缀，也支持裸文件路径）。
         * <p>配置后网关直接用本地公钥验签，不再访问 auth 服务，适合离线/单机调试。
         */
        private String publicKeyPath;
    }

    @Getter
    @Setter
    public static class Jwks {

        /** 可选：直连 auth 服务的 JWK 地址（如 http://127.0.0.1:8080/jwks）；留空则通过服务发现定位。 */
        private String uri;

        /** auth 服务名（服务发现用）。 */
        private String serviceName = AuthConstants.AUTH_SERVICE_NAME;

        /** 公钥刷新间隔，用于支持 auth 服务密钥轮换。 */
        private Duration refreshInterval = Duration.ofMinutes(10);

        /** 拉取 /jwks 的请求超时时间。 */
        private Duration timeout = Duration.ofSeconds(5);
    }
}
