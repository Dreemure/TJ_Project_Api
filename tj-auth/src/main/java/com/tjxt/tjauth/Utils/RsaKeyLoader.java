package com.tjxt.tjauth.Utils;


import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/*
 * RSA 密钥加载工具。
 * 职责：从 PEM 文件读取公钥/私钥，支持 classpath: 和 file: 两种前缀（也支持裸文件路径）。
 * 说明：
 *   - 未配置密钥路径、或配置的路径不可用时，回退到内置默认密钥（classpath:keys/*.pem）并打印告警，
 *     保证本地开发开箱可用；生产环境请通过 tj.auth.jwt.private-key-path / public-key-path 显式配置
 *   - 私钥为 PKCS#8（BEGIN PRIVATE KEY），公钥为 X.509（BEGIN PUBLIC KEY）
 */
@Slf4j
@Component
public class RsaKeyLoader {

    /** 内置默认私钥（仅供开发环境使用，与 docker/nacos/auth-service.yaml 的路径保持一致） */
    private static final String DEFAULT_PRIVATE_KEY_LOCATION = "classpath:keys/private_key.pem";
    /** 内置默认公钥（仅供开发环境使用，与 docker/nacos/auth-service.yaml 的路径保持一致） */
    private static final String DEFAULT_PUBLIC_KEY_LOCATION = "classpath:keys/public_key.pem";

    private final ResourceLoader resourceLoader;

    public RsaKeyLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /** 加载私钥（PKCS#8 PEM） */
    public PrivateKey loadPrivateKey(String location) {
        byte[] der = readDer(location, DEFAULT_PRIVATE_KEY_LOCATION, "私钥", "private-key-path");
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("解析私钥失败: " + location, e);
        }
    }

    /** 加载公钥（X.509 PEM） */
    public PublicKey loadPublicKey(String location) {
        byte[] der = readDer(location, DEFAULT_PUBLIC_KEY_LOCATION, "公钥", "public-key-path");
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("解析公钥失败: " + location, e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 读取并把 PEM 解码为 DER 字节。
     *
     * @param location        配置的位置；为空时使用默认位置
     * @param defaultLocation 内置默认位置
     * @param type            密钥类型（仅用于日志）
     * @param configKey       tj.auth.jwt 下的配置项名（仅用于日志）
     */
    private byte[] readDer(String location, String defaultLocation, String type, String configKey) {
        Exception failure = null;
        if (StringUtils.hasText(location)) {
            try {
                return decodePem(location);
            } catch (Exception e) {
                failure = e;
                log.warn("加载{}失败：{}（{}），尝试回退到内置默认密钥 {}",
                        type, location, e.getMessage(), defaultLocation);
            }
        } else {
            log.warn("未配置 tj.auth.jwt.{}，使用内置默认{}：{}", configKey, type, defaultLocation);
        }
        try {
            return decodePem(defaultLocation);
        } catch (Exception e) {
            if (failure != null) {
                e.addSuppressed(failure);
            }
            throw new IllegalStateException(
                    "加载%s失败：%s，且内置默认密钥不可用：%s".formatted(type, location, defaultLocation), e);
        }
    }

    /** 读取 PEM 文件，去掉头尾标记与空白后 Base64 解码 */
    private byte[] decodePem(String location) throws Exception {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("密钥文件不存在: " + location);
        }
        String content;
        try (var in = resource.getInputStream()) {
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String base64 = content
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");   // 去掉所有空白
        return Base64.getDecoder().decode(base64);
    }
}
