package com.tjxt.tjcommon.Sign;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/*
 * 请求签名校验配置（业务服务按需开启，默认关闭）。
 *
 * 配置示例（Nacos 里给需要的服务加）：
 *
 *   tj:
 *     auth:
 *       sign:
 *         enabled: true                       # 开关：默认 false，不开等于这套机制完全不存在
 *         include-path:                       # 只校验这些路径（ant 风格），其余请求原样放行
 *           - /v2/partner/**
 *         tolerance: 5m                       # 时间戳容差（双向）
 *         nonce-ttl: 10m                      # nonce 去重记录的保留时间
 *         max-body-bytes: 1048576             # 超过该大小直接拒绝（不做无上限缓冲）
 *         strip-path-prefix: /course          # 可选：验签方看到的路径前缀与签名方不一致时去掉（例如经网关 StripPrefix）
 *         apps:
 *           partner-a:                        # appId
 *             public-key: MFkwEwYHKoZIzj0C... # Base64(SPKI) 公钥（服务端固定，绝不来自请求）
 *           order-service:
 *             public-key-path: classpath:keys/order-service.pub.pem   # 也可用 PEM 文件（classpath:/file:）
 *
 * 安全说明：
 *   - 未配置 include-path 时不做任何校验（不会"看起来开了其实没生效"）；
 *   - 开启后，命中 include-path 的请求缺任何一个签名头 → 一律 401（fail-closed）；
 *   - 公钥只从配置读取，请求里带的公钥一概不用。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "tj.auth.sign")
public class ApiSignProperties {

    /** 是否开启请求签名校验（默认关闭） */
    private boolean enabled = false;

    /** 需要校验签名的路径（ant 风格），默认空 = 不校验任何路径 */
    private Set<String> includePath = new LinkedHashSet<>();

    /** 不需要校验的路径（优先级高于 include-path，便于放行健康检查等） */
    private Set<String> excludePath = new LinkedHashSet<>();

    /** 时间戳容差（双向：过期与"未来时间"都拒绝），默认 5 分钟 */
    private Duration tolerance = Duration.ofMinutes(5);

    /** nonce 去重记录保留时长，默认 10 分钟（应 >= 时间戳容差的 2 倍） */
    private Duration nonceTtl = Duration.ofMinutes(10);

    /** 允许缓冲的最大请求体字节数，默认 1MB；超过直接拒绝，避免无上限占用内存 */
    private int maxBodyBytes = 1024 * 1024;

    /** 可选：验签前从请求路径去掉的前缀（如网关 StripPrefix 场景） */
    private String stripPathPrefix;

    /** 调用方公钥表：appId -> 公钥 */
    private Map<String, AppKey> apps = new LinkedHashMap<>();

    /** 单个 appId 的密钥配置 */
    @Getter
    @Setter
    public static class AppKey {
        /** Base64(SPKI) 公钥 */
        private String publicKey;
        /** 或：PEM 文件位置（classpath: / file: / 裸路径） */
        private String publicKeyPath;
        /** 备注（谁在用这把密钥，便于运维核对） */
        private String remark;
    }
}
