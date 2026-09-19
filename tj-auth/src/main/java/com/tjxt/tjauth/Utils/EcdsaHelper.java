package com.tjxt.tjauth.Utils;

import lombok.Getter;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/*
 * ECDSA 签名验证工具类，兼容前端 Web Crypto API。
 *  - 曲线：P-256 / secp256r1
 *  - 公钥：Base64 编码的 SPKI（X.509 SubjectPublicKeyInfo）
 *  - 私钥：Base64 编码的 PKCS#8（如需导出时使用）
 *  - 签名：Base64 编码的 IEEE P1363（r || s 固定长度拼接），与 subtle.sign 输出一致
 *  - 哈希：SHA-256
 */
public final class EcdsaHelper {

    private static final String ALGORITHM = "EC";
    private static final String SIGN_ALGORITHM = "SHA256withECDSA";
    private static final String CURVE = "secp256r1"; // P-256
    private static final int P1363_HALF_LEN = 32;    // P-256 的 r / s 各 32 字节

    private EcdsaHelper() {
    }

    // ==================== 1. 生成密钥对 ====================

    /**
     * 生成 P-256 ECDSA 密钥对。
     */
    public static EcdsaKeyPair generateEcdsaKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(new ECGenParameterSpec(CURVE), new SecureRandom());
            KeyPair keyPair = generator.generateKeyPair();

            // 公钥 Base64（SPKI），可直接发给后端 / 前端
            String publicKeyBase64 = Base64.getEncoder()
                    .encodeToString(keyPair.getPublic().getEncoded());

            return new EcdsaKeyPair(keyPair.getPrivate(), keyPair.getPublic(), publicKeyBase64);
        } catch (Exception e) {
            throw new IllegalStateException("生成 ECDSA 密钥对失败", e);
        }
    }

    // ==================== 2. 用私钥签名 ====================

    /**
     * 用私钥对参数签名，输出 P1363 格式的 Base64，和前端 subtle.sign 结果一致。
     */
    public static String sign(String rawPayload, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initSign(privateKey, new SecureRandom());
            signature.update(rawPayload.getBytes(StandardCharsets.UTF_8));
            byte[] derSig = signature.sign();
            byte[] p1363Sig = derToP1363(derSig);
            return Base64.getEncoder().encodeToString(p1363Sig);
        } catch (Exception e) {
            throw new IllegalStateException("ECDSA 签名失败", e);
        }
    }

    /**
     * 便捷重载：直接用密钥对里的私钥签名。
     */
    public static String sign(String rawPayload, EcdsaKeyPair keyPair) {
        return sign(rawPayload, keyPair.getPrivateKey());
    }

    // ==================== 3. 验签 ====================

    /**
     * 用公钥验签。
     *
     * @param publicKeyBase64 公钥（Base64 编码的 SPKI）
     * @param signatureBase64 签名（Base64 编码的 IEEE P1363）
     * @param rawPayload      原始字符串
     */
    public static boolean verifySignature(String publicKeyBase64,
                                          String signatureBase64,
                                          String rawPayload) {
        try {
            byte[] pubKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            byte[] p1363Sig = Base64.getDecoder().decode(signatureBase64);
            byte[] payloadBytes = rawPayload.getBytes(StandardCharsets.UTF_8);

            PublicKey publicKey = KeyFactory.getInstance(ALGORITHM)
                    .generatePublic(new X509EncodedKeySpec(pubKeyBytes));

            byte[] derSig = p1363ToDer(p1363Sig);

            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(payloadBytes);
            return signature.verify(derSig);
        } catch (Exception e) {
            // log.error("ECDSA 验签异常", e);
            return false;
        }
    }

    // ==================== 密钥对封装 ====================

    @Getter
    public static final class EcdsaKeyPair {
        /**
         * -- GETTER --
         * 用于签名的私钥（Java 对象，对应前端的 CryptoKey privateKey）
         */
        private final PrivateKey privateKey;
        /**
         * -- GETTER --
         * 公钥对象（对应前端的 CryptoKey publicKey）
         */
        private final PublicKey publicKey;
        /**
         * -- GETTER --
         * Base64 (SPKI) 公钥字符串，可直接发给后端 / 前端
         */
        private final String publicKeyBase64;

        public EcdsaKeyPair(PrivateKey privateKey, PublicKey publicKey, String publicKeyBase64) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.publicKeyBase64 = publicKeyBase64;
        }

        /**
         * 可选：导出私钥为 Base64 (PKCS#8)，一般不需要，对齐前端不导出私钥的做法
         */
        public String getPrivateKeyBase64() {
            return Base64.getEncoder().encodeToString(privateKey.getEncoded());
        }
    }

    // ==================== 内部工具：DER <-> P1363 ====================

    private static byte[] p1363ToDer(byte[] p1363) {
        if (p1363 == null || p1363.length == 0 || (p1363.length & 1) != 0) {
            throw new IllegalArgumentException("Invalid P1363 signature length: " +
                    (p1363 == null ? "null" : p1363.length));
        }
        int halfLen = p1363.length / 2;

        byte[] rBytes = new byte[halfLen];
        byte[] sBytes = new byte[halfLen];
        System.arraycopy(p1363, 0, rBytes, 0, halfLen);
        System.arraycopy(p1363, halfLen, sBytes, 0, halfLen);

        BigInteger r = new BigInteger(1, rBytes);
        BigInteger s = new BigInteger(1, sBytes);

        byte[] rDer = toDerInteger(r);
        byte[] sDer = toDerInteger(s);

        int seqLen = rDer.length + sDer.length;
        byte[] der = new byte[2 + seqLen];
        int idx = 0;
        der[idx++] = 0x30;
        der[idx++] = (byte) seqLen;
        System.arraycopy(rDer, 0, der, idx, rDer.length);
        idx += rDer.length;
        System.arraycopy(sDer, 0, der, idx, sDer.length);
        return der;
    }

    private static byte[] derToP1363(byte[] der) {
        if (der == null || der.length < 8 || der[0] != 0x30) {
            throw new IllegalArgumentException("Invalid DER signature");
        }

        int idx = 1;
        int seqLen = der[idx++] & 0xFF;
        if ((seqLen & 0x80) != 0) {
            int num = seqLen & 0x7F;
            seqLen = 0;
            for (int i = 0; i < num; i++) {
                seqLen = (seqLen << 8) | (der[idx++] & 0xFF);
            }
        }

        if (der[idx++] != 0x02) {
            throw new IllegalArgumentException("Invalid DER signature: r not INTEGER");
        }
        int rLen = der[idx++] & 0xFF;
        byte[] rBytes = Arrays.copyOfRange(der, idx, idx + rLen);
        idx += rLen;

        if (der[idx++] != 0x02) {
            throw new IllegalArgumentException("Invalid DER signature: s not INTEGER");
        }
        int sLen = der[idx++] & 0xFF;
        byte[] sBytes = Arrays.copyOfRange(der, idx, idx + sLen);

        BigInteger r = new BigInteger(1, rBytes);
        BigInteger s = new BigInteger(1, sBytes);

        byte[] p1363 = new byte[P1363_HALF_LEN * 2];
        System.arraycopy(toFixedLength(r, P1363_HALF_LEN), 0, p1363, 0, P1363_HALF_LEN);
        System.arraycopy(toFixedLength(s, P1363_HALF_LEN), 0, p1363, P1363_HALF_LEN, P1363_HALF_LEN);
        return p1363;
    }

    private static byte[] toFixedLength(BigInteger value, int len) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        if (bytes.length > len) {
            bytes = Arrays.copyOfRange(bytes, bytes.length - len, bytes.length);
        }
        byte[] result = new byte[len];
        System.arraycopy(bytes, 0, result, len - bytes.length, bytes.length);
        return result;
    }

    private static byte[] toDerInteger(BigInteger value) {
        byte[] bytes = value.toByteArray();
        int len = bytes.length;
        byte[] result = new byte[2 + len];
        result[0] = 0x02;
        result[1] = (byte) len;
        System.arraycopy(bytes, 0, result, 2, len);
        return result;
    }

    /**
     * 从 Base64(PKCS#8) 字符串导入私钥。仅用于测试。
     */
    public static PrivateKey importPrivateKey(String privateKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            return KeyFactory.getInstance(ALGORITHM)
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new IllegalStateException("导入私钥失败", e);
        }
    }
}