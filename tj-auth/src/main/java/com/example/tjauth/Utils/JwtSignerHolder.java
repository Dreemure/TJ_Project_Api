package com.example.tjauth.Utils;


import com.alibaba.nacos.common.utils.CollectionUtils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * JWT 公钥持有者（RS256 模式）。
 * 职责：从 auth-service 的 /jwks 端点动态加载 RSA 公钥，供 JWT 验签使用。
 * 使用：其他服务通过本类获取公钥，构造 JWTVerifier 验签。
 */
@Slf4j
@Component
public class JwtSignerHolder implements RSAKeyProvider {

    private volatile RSAPublicKey publicKey;

    private final DiscoveryClient discoveryClient;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            r -> new Thread(r, "AuthFetchJwkThread")
    );

    public JwtSignerHolder(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @PostConstruct
    public void init() {
        executor.submit(this::loadPublicKey);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        log.debug("销毁加载秘钥线程 AuthFetchJwkThread");
    }

    /**
     * 通过 DiscoveryClient 找到 auth-service，拉取 JWK 公钥。
     */
    private void loadPublicKey() {
        while (publicKey == null) {
            try {
                List<ServiceInstance> instances = discoveryClient.getInstances("auth-service");
                if (CollectionUtils.isEmpty(instances)) {
                    log.warn("未发现 auth-service 实例，10 秒后重试");
                    Thread.sleep(10_000);
                    continue;
                }
                ServiceInstance instance = instances.getFirst();
                String jwksUri = String.format("http://%s:%d/jwks", instance.getHost(), instance.getPort());
                log.info("加载 JWK 地址: {}", jwksUri);

                String jwkJson = fetchJwks(jwksUri);
                publicKey = parseRsaPublicKey(jwkJson);
                log.info("JWK 公钥加载成功");
            } catch (Exception e) {
                log.error("加载 JWK 失败: {}", e.getMessage());
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private String fetchJwks(String jwksUri) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(jwksUri)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private RSAPublicKey parseRsaPublicKey(String jwkJson) throws Exception {
        // 解析 JWK JSON → RSAPublicKey
        // 可以用 nimbus-jose-jwt 库：
        //   JWK jwk = JWK.parse(jwkJson);
        //   return jwk.toRSAKey().toRSAPublicKey();
        // 这里略，因为 Auth0 java-jwt 本身不带 JWK 解析，通常配合 nimbus 使用
        throw new UnsupportedOperationException("请集成 nimbus-jose-jwt 或 Auth0 的 jwks-rsa 库");
    }

    // ==================== RSAKeyProvider 实现 ====================

    @Override
    public RSAPublicKey getPublicKeyById(String keyId) {
        return publicKey;
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
        return null;   // 验签方不需要私钥
    }

    @Override
    public String getPrivateKeyId() {
        return null;
    }

    /**
     * 获取已加载的 JWTVerifier。
     */
    public JWTVerifier getVerifier(String issuer) {
        if (publicKey == null) {
            throw new IllegalStateException("JWK 公钥尚未加载");
        }
        return JWT.require(Algorithm.RSA256(this))
                .withIssuer(issuer)
                .build();
    }
}