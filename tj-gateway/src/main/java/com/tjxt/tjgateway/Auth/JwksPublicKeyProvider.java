package com.tjxt.tjgateway.Auth;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.tjxt.tjcommon.Constants.AuthConstants;
import com.tjxt.tjgateway.Config.AuthProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 * JWT 公钥提供者（RS256 验签）。
 * 职责：为网关提供验签用的 RSA 公钥，来源优先级：
 *   1. 本地公钥 PEM（tj.auth.jwt.public-key-path，配置后不再访问 auth 服务）
 *   2. auth 服务的 /jwks 端点（通过服务发现定位，直连地址可用 tj.auth.jwks.uri 覆盖）
 * 说明：
 *   - 启动时后台线程拉取公钥（不阻塞网关启动，拉不到就不停重试，与原项目 JwtSignerHolder 行为一致）
 *   - 定时任务按 tj.auth.jwks.refresh-interval 重新拉取，支持 auth 服务密钥轮换
 *   - 验签失败时可由 JwtTokenVerifier 调用 {@link #refreshNow()} 立即重试（带节流，避免被刷）
 *   - 兼容两种 /jwks 响应：标准 JWK Set（{"keys":[{n,e}]}）与 Base64 编码的 X.509 DER 公钥（老版本格式）
 */
@Slf4j
@Component
public class JwksPublicKeyProvider {

    /** 拉取失败后的重试间隔（同时用于验签失败时的主动刷新节流） */
    private static final long RETRY_INTERVAL_MS = 30_000L;

    private final AuthProperties authProperties;
    private final ObjectProvider<DiscoveryClient> discoveryClientProvider;
    private final ResourceLoader resourceLoader;

    /** 后台加载线程（守护线程，避免影响 JVM 退出） */
    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "AuthFetchJwkThread");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * -- GETTER --
     *  获取当前验签公钥。
     *
     * @return RSA 公钥；尚未加载成功时返回 null
     */
    @Getter
    private volatile RSAPublicKey publicKey;
    private volatile long lastLoadedAt;
    private volatile long lastAttemptAt;
    private volatile HttpClient httpClient;

    public JwksPublicKeyProvider(AuthProperties authProperties,
                                 ObjectProvider<DiscoveryClient> discoveryClientProvider,
                                 ResourceLoader resourceLoader) {
        this.authProperties = authProperties;
        this.discoveryClientProvider = discoveryClientProvider;
        this.resourceLoader = resourceLoader;
    }

    // ==================== 生命周期 ====================

    @PostConstruct
    public void init() {
        // 1. 优先使用本地公钥
        String publicKeyPath = authProperties.getJwt().getPublicKeyPath();
        if (StringUtils.hasText(publicKeyPath)) {
            try {
                this.publicKey = loadPublicKeyFromPem(publicKeyPath);
                this.lastLoadedAt = System.currentTimeMillis();
                log.info("网关已加载本地 JWT 公钥：{}", publicKeyPath);
                return;
            } catch (Exception e) {
                log.error("加载本地 JWT 公钥失败，将回退到 auth 服务 /jwks：{}", publicKeyPath, e);
            }
        }
        // 2. 后台线程拉取 auth 服务的公钥
        loader.submit(this::loadFromAuthService);
    }

    @PreDestroy
    public void destroy() {
        loader.shutdownNow();
        log.debug("销毁加载 JWT 公钥线程 AuthFetchJwkThread");
    }

    /**
     * 定时刷新公钥（支持密钥轮换）；使用本地公钥时无需刷新。
     */
    @Scheduled(fixedDelay = 10_000)
    public void refreshTask() {
        if (useStaticPublicKey()) {
            return;
        }
        long now = System.currentTimeMillis();
        long interval = authProperties.getJwks().getRefreshInterval().toMillis();
        if (publicKey != null && now - lastLoadedAt < interval) {
            return;
        }
        refreshNow();
    }

    /**
     * 从 auth 服务立即刷新公钥（带节流）。
     * <p>供验签失败时主动重试，用于感知 auth 服务密钥轮换。
     *
     * @return true 表示本次成功更新了公钥
     */
    public boolean refreshNow() {
        if (useStaticPublicKey()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastAttemptAt < RETRY_INTERVAL_MS) {
            return false;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now - lastAttemptAt < RETRY_INTERVAL_MS) {
                return false;
            }
            lastAttemptAt = now;
            try {
                RSAPublicKey key = fetchPublicKey();
                if (key == null) {
                    return false;
                }
                this.publicKey = key;
                this.lastLoadedAt = now;
                log.info("网关已加载 auth 服务 JWT 公钥");
                return true;
            } catch (Exception e) {
                log.warn("拉取 auth 服务 JWT 公钥失败：{}", e.getMessage());
                return false;
            }
        }
    }

    // ==================== 公钥加载 ====================

    /**
     * 后台线程：反复尝试从 auth 服务拉取公钥，直到成功为止。
     */
    private void loadFromAuthService() {
        while (publicKey == null && !Thread.currentThread().isInterrupted()) {
            if (useStaticPublicKey()) {
                return;
            }
            try {
                RSAPublicKey key = fetchPublicKey();
                if (key != null) {
                    this.publicKey = key;
                    this.lastLoadedAt = System.currentTimeMillis();
                    log.info("网关已加载 auth 服务 JWT 公钥");
                    return;
                }
            } catch (Exception e) {
                log.error("拉取 auth 服务 JWT 公钥失败，10 秒后重试：{}", e.getMessage());
            }
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * 拉取并解析 auth 服务的 JWK 公钥。
     */
    private RSAPublicKey fetchPublicKey() throws Exception {
        String jwksUri = resolveJwksUri();
        if (jwksUri == null) {
            log.debug("暂未发现 {} 实例，无法拉取 JWT 公钥", authProperties.getJwks().getServiceName());
            return null;
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(jwksUri))
                .timeout(authProperties.getJwks().getTimeout())
                .GET()
                .build();
        HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200 || !StringUtils.hasText(response.body())) {
            log.warn("拉取 JWK 失败：uri={}, status={}", jwksUri, response.statusCode());
            return null;
        }
        return parsePublicKey(response.body().trim());
    }

    /**
     * 解析公钥响应体：兼容标准 JWK Set 与 Base64(X.509 DER) 两种格式。
     */
    private RSAPublicKey parsePublicKey(String body) throws Exception {
        if (body.startsWith("{")) {
            JSONArray keys = JSON.parseObject(body).getJSONArray("keys");
            if (CollectionUtils.isEmpty(keys)) {
                throw new IllegalStateException("JWK 数据为空");
            }
            JSONObject jwk = keys.getJSONObject(0);
            String modulus = jwk.getString("n");
            String exponent = jwk.getString("e");
            if (!StringUtils.hasText(modulus) || !StringUtils.hasText(exponent)) {
                throw new IllegalStateException("JWK 缺少 n/e 参数");
            }
            Base64.Decoder decoder = Base64.getUrlDecoder();
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                    new RSAPublicKeySpec(
                            new BigInteger(1, decoder.decode(modulus)),
                            new BigInteger(1, decoder.decode(exponent))));
        }
        // 兼容老格式：响应体是 Base64 编码的 X.509 公钥
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(body.replaceAll("\\s", ""))));
    }

    /**
     * 解析 auth 服务地址：优先使用显式配置的 uri，其次走服务发现。
     */
    private String resolveJwksUri() {
        String uri = authProperties.getJwks().getUri();
        if (StringUtils.hasText(uri)) {
            return uri;
        }
        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient == null) {
            return null;
        }
        List<ServiceInstance> instances = discoveryClient.getInstances(authProperties.getJwks().getServiceName());
        if (CollectionUtils.isEmpty(instances)) {
            return null;
        }
        ServiceInstance instance = instances.getFirst();
        return "http://%s:%d%s".formatted(instance.getHost(), instance.getPort(), AuthConstants.JWKS_PATH);
    }

    /**
     * 从本地 PEM 文件加载公钥。
     */
    private RSAPublicKey loadPublicKeyFromPem(String location) throws Exception {
        String pem;
        try (var in = toResource(location).getInputStream()) {
            pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }

    /**
     * 把配置的路径转换为 Resource：支持 classpath:/file: 前缀与裸文件路径。
     */
    private Resource toResource(String location) {
        if (location.startsWith("classpath:") || location.startsWith("file:")
                || location.startsWith("http://") || location.startsWith("https://")) {
            return resourceLoader.getResource(location);
        }
        Path path = Path.of(location);
        return Files.exists(path) ? new FileSystemResource(path) : resourceLoader.getResource("classpath:" + location);
    }

    /**
     * 是否使用本地静态公钥（配置了 public-key-path）。
     */
    private boolean useStaticPublicKey() {
        return StringUtils.hasText(authProperties.getJwt().getPublicKeyPath());
    }

    private HttpClient httpClient() {
        HttpClient client = this.httpClient;
        if (client == null) {
            synchronized (this) {
                if (this.httpClient == null) {
                    this.httpClient = HttpClient.newBuilder()
                            .connectTimeout(authProperties.getJwks().getTimeout())
                            .build();
                }
                client = this.httpClient;
            }
        }
        return client;
    }

    /** 预留：便于排查公钥加载状态（HealthIndicator 可复用）。 */
    public Duration loadedAge() {
        return lastLoadedAt == 0 ? null : Duration.ofMillis(System.currentTimeMillis() - lastLoadedAt);
    }
}
