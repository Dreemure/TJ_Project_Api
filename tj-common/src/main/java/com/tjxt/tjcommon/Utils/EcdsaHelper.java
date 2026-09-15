package com.tjxt.tjcommon.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/*
 * ECDSA 签名验证工具类。
 * 职责：验证前端 Web Crypto API 生成的 ECDSA 签名。
 * 说明：
 *   - 公钥为 Base64 编码的 SPKI（X.509 SubjectPublicKeyInfo）格式
 *   - 签名为 Base64 编码的 IEEE P1363（r || s 固定长度拼接）格式
 *   - Java 的 Signature 默认使用 DER 编码，需要把 P1363 转为 DER 后再验证
 *   - 哈希算法：SHA-256
 */
public final class EcdsaHelper {

    private static final String ALGORITHM = "EC";
    private static final String SIGN_ALGORITHM = "SHA256withECDSA";

    private EcdsaHelper() {
        // 工具类私有构造
    }

    /**
     * 验证 ECDSA 签名。
     *
     * @param publicKeyBase64 公钥（Base64 编码的 SPKI 格式）
     * @param signatureBase64 签名（Base64 编码的 IEEE P1363 格式）
     * @param rawPayload      原始字符串（请求 Body 的原始 JSON 字符串）
     * @return 验签是否成功；任何异常都返回 false
     */
    public static boolean verifySignature(String publicKeyBase64,
                                          String signatureBase64,
                                          String rawPayload) {
        try {
            // 1. Base64 解码
            byte[] pubKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            byte[] p1363Sig = Base64.getDecoder().decode(signatureBase64);
            byte[] payloadBytes = rawPayload.getBytes(StandardCharsets.UTF_8);

            // 2. 导入 SPKI 格式公钥
            PublicKey publicKey = KeyFactory.getInstance(ALGORITHM)
                    .generatePublic(new X509EncodedKeySpec(pubKeyBytes));

            // 3. P1363 → DER（Java 的 Signature 默认用 DER 格式验签）
            byte[] derSig = p1363ToDer(p1363Sig);

            // 4. 验证签名
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(payloadBytes);
            return signature.verify(derSig);
        } catch (Exception e) {
            // 记录日志：Base64 解码失败、密钥格式错误、签名格式错误等
            // log.error("ECDSA 验签异常", e);
            return false;
        }
    }

    /**
     * 把 IEEE P1363 格式的签名转换为 DER 格式。
     * <p>P1363 格式：r || s，固定长度拼接（P-256 曲线各 32 字节，共 64 字节）
     * <p>DER 格式：SEQUENCE { INTEGER r, INTEGER s }
     */
    private static byte[] p1363ToDer(byte[] p1363) {
        if (p1363 == null || p1363.length == 0 || (p1363.length & 1) != 0) {
            throw new IllegalArgumentException("Invalid P1363 signature length: " +
                    (p1363 == null ? "null" : p1363.length));
        }
        int halfLen = p1363.length / 2;

        // 拆出 r 和 s（转成 BigInteger 自动去掉前导 0）
        byte[] rBytes = new byte[halfLen];
        byte[] sBytes = new byte[halfLen];
        System.arraycopy(p1363, 0, rBytes, 0, halfLen);
        System.arraycopy(p1363, halfLen, sBytes, 0, halfLen);

        BigInteger r = new BigInteger(1, rBytes);
        BigInteger s = new BigInteger(1, sBytes);

        // 构造 DER 编码：SEQUENCE { INTEGER r, INTEGER s }
        byte[] rDer = toDerInteger(r);
        byte[] sDer = toDerInteger(s);

        int seqLen = rDer.length + sDer.length;
        byte[] der = new byte[2 + seqLen];
        int idx = 0;
        der[idx++] = 0x30; // SEQUENCE
        der[idx++] = (byte) seqLen;
        System.arraycopy(rDer, 0, der, idx, rDer.length);
        idx += rDer.length;
        System.arraycopy(sDer, 0, der, idx, sDer.length);
        return der;
    }

    /**
     * 把 BigInteger 编码为 DER INTEGER。
     * <p>如果最高位是 1（会被解析为负数），需要在前面补一个 0x00。
     */
    private static byte[] toDerInteger(BigInteger value) {
        byte[] bytes = value.toByteArray();
        int len = bytes.length;
        byte[] result = new byte[2 + len];
        result[0] = 0x02; // INTEGER
        result[1] = (byte) len;
        System.arraycopy(bytes, 0, result, 2, len);
        return result;
    }
}
