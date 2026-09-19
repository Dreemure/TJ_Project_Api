package com.tjxt.tjcommon.Utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * EcdsaHelper 单元测试（纯 JUnit，不启动 Spring 容器）。
 * 职责：把"防篡改"能力固化成回归防线 —— 任何对验签逻辑、格式约定、fail-closed 行为的改动都必须让这里保持全绿。
 * 覆盖：
 *   1. 签名 / 验签往返，以及 P1363 必须是 64 字节的前端契约（与 Web Crypto subtle.sign 一致）
 *   2. 篡改载荷、篡改签名、换公钥一律拒绝
 *   3. 畸形输入 fail-closed（返回 false 且不抛异常），长度不合法直接拒收，不进入 DER 解析
 *   4. 签名随机化（同一载荷两次签名不同，但都可验签）
 *   5. 公钥取自调用方时验签形同虚设（反例说明：公钥必须在服务端固定）
 */
@DisplayName("ECDSA 签名工具 EcdsaHelper")
class EcdsaHelperTest {

    /** 模拟服务端↔服务端调用的待签串：业务上下文 + 时间戳 + nonce + body 摘要 */
    private static final String PAYLOAD =
            "appId=tj-course&method=POST&path=/v2/orders&ts=1730000000&nonce=8f2a9c1e&bodySha256=2b1c9f";

    private static final int P1363_LEN = 64;

    @Test
    @DisplayName("签名与验签往返通过，且签名是 64 字节的 P1363（前端可直接验）")
    void signAndVerifyRoundTrip() {
        EcdsaHelper.EcdsaKeyPair keyPair = EcdsaHelper.generateEcdsaKeys();

        String signature = EcdsaHelper.sign(PAYLOAD, keyPair);

        assertEquals(P1363_LEN, Base64.getDecoder().decode(signature).length,
                "P-256 的 P1363 签名必须是 r‖s 共 64 字节，否则前端 Web Crypto 无法验签");
        assertTrue(EcdsaHelper.verifySignature(keyPair.getPublicKeyBase64(), signature, PAYLOAD));
    }

    @Test
    @DisplayName("篡改载荷一律验签失败")
    void tamperedPayloadIsRejected() {
        EcdsaHelper.EcdsaKeyPair keyPair = EcdsaHelper.generateEcdsaKeys();
        String signature = EcdsaHelper.sign(PAYLOAD, keyPair);

        assertFalse(EcdsaHelper.verifySignature(keyPair.getPublicKeyBase64(), signature, PAYLOAD + " "),
                "末尾多一个空格也必须失败");
        assertFalse(EcdsaHelper.verifySignature(keyPair.getPublicKeyBase64(), signature,
                        PAYLOAD.replace("appId=tj-course", "appId=tj-pay")),
                "改掉业务上下文必须失败");
        assertFalse(EcdsaHelper.verifySignature(keyPair.getPublicKeyBase64(), signature,
                        PAYLOAD.replace("ts=1730000000", "ts=1730000001")),
                "改掉时间戳必须失败");
        assertFalse(EcdsaHelper.verifySignature(keyPair.getPublicKeyBase64(), signature,
                        PAYLOAD.trim().toUpperCase()),
                "大小写变化必须失败");
    }

    @Test
    @DisplayName("篡改签名的任何一个字节都验签失败")
    void tamperedSignatureIsRejected() {
        EcdsaHelper.EcdsaKeyPair keyPair = EcdsaHelper.generateEcdsaKeys();
        byte[] raw = Base64.getDecoder().decode(EcdsaHelper.sign(PAYLOAD, keyPair));

        for (int index : new int[]{0, 31, 32, 63}) {
            byte[] tampered = raw.clone();
            tampered[index] ^= 0x01;
            assertFalse(EcdsaHelper.verifySignature(keyPair.getPublicKeyBase64(),
                            Base64.getEncoder().encodeToString(tampered), PAYLOAD),
                    "第 " + index + " 字节被改动后必须验签失败");
        }
    }

    @Test
    @DisplayName("换一把公钥验签失败（签名只能被签发方的公钥验证）")
    void signatureFromAnotherKeyIsRejected() {
        EcdsaHelper.EcdsaKeyPair signer = EcdsaHelper.generateEcdsaKeys();
        EcdsaHelper.EcdsaKeyPair other = EcdsaHelper.generateEcdsaKeys();
        String signature = EcdsaHelper.sign(PAYLOAD, signer);

        assertFalse(EcdsaHelper.verifySignature(other.getPublicKeyBase64(), signature, PAYLOAD));
    }

    @Test
    @DisplayName("畸形输入 fail-closed：返回 false 且不抛异常")
    void malformedInputIsRejectedWithoutException() throws Exception {
        EcdsaHelper.EcdsaKeyPair keyPair = EcdsaHelper.generateEcdsaKeys();
        String publicKey = keyPair.getPublicKeyBase64();
        String validSignature = EcdsaHelper.sign(PAYLOAD, keyPair);

        // 长度不合法：空、过短、奇数长度、63/65/128 字节 —— 严格拒收，不进入 DER 转换
        for (byte[] badSignature : new byte[][]{
                new byte[0], new byte[4], new byte[5], new byte[63], new byte[65], new byte[128]}) {
            assertFalse(EcdsaHelper.verifySignature(publicKey,
                            Base64.getEncoder().encodeToString(badSignature), PAYLOAD),
                    "长度 " + badSignature.length + " 字节的签名必须被拒收");
        }

        // 长度合法但内容无效（全 0）
        assertFalse(EcdsaHelper.verifySignature(publicKey,
                Base64.getEncoder().encodeToString(new byte[P1363_LEN]), PAYLOAD));

        // 格式非法：Base64、公钥类型、null
        assertFalse(EcdsaHelper.verifySignature("not-base64!!!", validSignature, PAYLOAD));
        assertFalse(EcdsaHelper.verifySignature(rsaPublicKeyBase64(), validSignature, PAYLOAD),
                "RSA 公钥不能当作 EC 公钥使用");
        assertFalse(EcdsaHelper.verifySignature(publicKey, "not-base64!!!", PAYLOAD));
        assertFalse(EcdsaHelper.verifySignature(null, validSignature, PAYLOAD));
        assertFalse(EcdsaHelper.verifySignature(publicKey, null, PAYLOAD));
        assertFalse(EcdsaHelper.verifySignature(publicKey, validSignature, null));
        assertFalse(EcdsaHelper.verifySignature("", validSignature, PAYLOAD));
    }

    @Test
    @DisplayName("ECDSA 签名是随机化的：同一载荷两次签名不同，但都能验签")
    void signatureIsRandomized() {
        EcdsaHelper.EcdsaKeyPair keyPair = EcdsaHelper.generateEcdsaKeys();

        String first = EcdsaHelper.sign(PAYLOAD, keyPair);
        String second = EcdsaHelper.sign(PAYLOAD, keyPair);

        assertNotEquals(first, second, "签名使用了随机 k，两次结果不应相同（相同则说明随机源有问题）");
        assertTrue(EcdsaHelper.verifySignature(keyPair.getPublicKeyBase64(), first, PAYLOAD));
        assertTrue(EcdsaHelper.verifySignature(keyPair.getPublicKeyBase64(), second, PAYLOAD));
    }

    @Test
    @DisplayName("UTF-8 中文载荷与超大载荷都能正确签名验签")
    void utf8AndLargePayloadAreSupported() {
        EcdsaHelper.EcdsaKeyPair keyPair = EcdsaHelper.generateEcdsaKeys();
        String chinese = "订单号=TJ20250101&备注=这是一笔测试订单";
        String large = "x".repeat(100_000);

        assertTrue(EcdsaHelper.verifySignature(keyPair.getPublicKeyBase64(),
                EcdsaHelper.sign(chinese, keyPair), chinese));
        assertTrue(EcdsaHelper.verifySignature(keyPair.getPublicKeyBase64(),
                EcdsaHelper.sign(large, keyPair), large));
        assertFalse(EcdsaHelper.verifySignature(keyPair.getPublicKeyBase64(),
                EcdsaHelper.sign(chinese, keyPair), large));
    }

    @Test
    @DisplayName("反例说明：公钥若取自调用方，攻击者可自行签名并验签通过（所以公钥必须服务端固定）")
    void callerSuppliedPublicKeyMakesVerificationMeaningless() {
        // 攻击者自己生成一对密钥，用"自己的私钥 + 篡改后的数据 + 自己的公钥"提交
        EcdsaHelper.EcdsaKeyPair attacker = EcdsaHelper.generateEcdsaKeys();
        String forgedPayload = PAYLOAD.replace("appId=tj-course", "appId=tj-pay");
        String forgedSignature = EcdsaHelper.sign(forgedPayload, attacker);

        // 只要验签方使用请求里的公钥，验签就会"通过"—— 这就是不能把 publicKey 交给调用方的原因
        assertTrue(EcdsaHelper.verifySignature(attacker.getPublicKeyBase64(), forgedSignature, forgedPayload),
                "公钥来自请求时验签形同虚设");

        // 服务端固定真实公钥后，同样的伪造请求验签失败
        EcdsaHelper.EcdsaKeyPair serverKeyPair = EcdsaHelper.generateEcdsaKeys();
        assertFalse(EcdsaHelper.verifySignature(serverKeyPair.getPublicKeyBase64(), forgedSignature, forgedPayload));
    }

    /** 生成一把 RSA 公钥（Base64/SPKI），用于验证"非 EC 公钥不得被接受" */
    private static String rsaPublicKeyBase64() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return Base64.getEncoder().encodeToString(generator.generateKeyPair().getPublic().getEncoded());
    }
}
