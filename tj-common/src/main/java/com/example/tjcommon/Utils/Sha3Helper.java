package com.example.tjcommon.Utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/*
 * SHA3-256 哈希工具类。
 * 职责：计算字符串的 SHA3-256 哈希值，输出小写十六进制字符串，与前端 js-sha3 保持一致。
 * 说明：SHA3-256 算法由 JDK 9+ 的 MessageDigest 原生支持，无需额外依赖。
 */
public final class Sha3Helper {

    private static final String ALGORITHM = "SHA3-256";

    private Sha3Helper() {
        // 工具类私有构造
    }

    /**
     * 计算字符串的 SHA3-256 哈希值。
     *
     * @param rawData 原始字符串
     * @return 小写的十六进制字符串（与前端 js-sha3 保持一致）
     * @throws IllegalArgumentException 参数为 null 时抛出
     * @throws IllegalStateException    当前 JDK 不支持 SHA3-256 时抛出
     */
    public static String computeSha3_256(String rawData) {
        if (rawData == null) {
            throw new IllegalArgumentException("rawData must not be null");
        }
        try {
            // 1. 获取 SHA3-256 摘要器
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);

            // 2. 计算哈希（UTF-8 编码，与 C# Encoding.UTF8 一致）
            byte[] hashBytes = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));

            // 3. 转小写十六进制（JDK 17+ 用 HexFormat，性能更好）
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA3-256，请使用 JDK 9+", e);
        }
    }
}