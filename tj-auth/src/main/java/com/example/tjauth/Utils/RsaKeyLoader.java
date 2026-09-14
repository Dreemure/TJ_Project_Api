package com.example.tjauth.Utils;


import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/*
 * RSA 密钥加载工具。
 * 职责：从 PEM 文件读取公钥/私钥，支持 classpath: 和 file: 两种前缀。
 */
@Slf4j
@Component
public class RsaKeyLoader {

    private final ResourceLoader resourceLoader;

    public RsaKeyLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /** 加载私钥（PKCS#8 PEM） */
    public PrivateKey loadPrivateKey(String location) {
        try {
            String pem = readPem(location);
            byte[] der = Base64.getDecoder().decode(pem);
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("加载私钥失败: " + location, e);
        }
    }

    /** 加载公钥（X.509 PEM） */
    public PublicKey loadPublicKey(String location) {
        try {
            String pem = readPem(location);
            byte[] der = Base64.getDecoder().decode(pem);
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("加载公钥失败: " + location, e);
        }
    }

    /** 读取 PEM 文件，去掉头尾标记和换行 */
    private String readPem(String location) throws Exception {
        Resource resource = resourceLoader.getResource(location);
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return content
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");   // 去掉所有空白
    }
}