package com.tjxt.tjcommon.Utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * 待签串（canonical）契约测试。
 * 这些断言是"签名方与验签方看到同一份字节"的固化约定：改动格式等于破坏线上协议，必须让这里一起改。
 */
@DisplayName("请求签名待签串契约")
class ApiSignHelperTest {

    @Test
    @DisplayName("待签串是固定 6 段、\\n 分隔，顺序与内容稳定")
    void canonicalStringIsStable() {
        String canonical = ApiSignHelper.canonicalString(
                "partner-a", "post", "/v2/partner/orders", 1730000000L, "abcd1234efgh", "deadbeef");

        assertEquals("partner-a\nPOST\n/v2/partner/orders\n1730000000\nabcd1234efgh\ndeadbeef", canonical);
        assertEquals(6, canonical.split("\n", -1).length);
        // method 统一大写：GET/get 不应产生两种签名
        assertEquals(canonical, ApiSignHelper.canonicalString(
                "partner-a", "POST", "/v2/partner/orders", 1730000000L, "abcd1234efgh", "deadbeef"));
    }

    @Test
    @DisplayName("空请求体摘要固定为常量（双方可核对）")
    void emptyBodyDigestIsConstant() {
        assertEquals(ApiSignHelper.EMPTY_BODY_SHA256, ApiSignHelper.sha256Hex(new byte[0]));
        assertEquals(ApiSignHelper.EMPTY_BODY_SHA256, ApiSignHelper.sha256Hex((byte[]) null));
    }

    @Test
    @DisplayName("签名字节一变，摘要就变（身体完整性由摘要承载）")
    void digestChangesWithBody() {
        assertNotEquals(ApiSignHelper.sha256Hex("{\"a\":1}"), ApiSignHelper.sha256Hex("{\"a\":2}"));
        // JSON 的键序/空格变化同样必须导致签名不同，这正是"不要重新序列化"的原因
        assertNotEquals(ApiSignHelper.sha256Hex("{\"a\":1,\"b\":2}"), ApiSignHelper.sha256Hex("{\"b\":2,\"a\":1}"));
    }

    @Test
    @DisplayName("时间戳容差是双向的：过期的与未来时间都拒绝")
    void timestampToleranceIsBidirectional() {
        long now = Instant.now().getEpochSecond();
        Duration tolerance = Duration.ofMinutes(5);

        assertTrue(ApiSignHelper.timestampFresh(now, tolerance));
        assertTrue(ApiSignHelper.timestampFresh(now - 299, tolerance));
        assertFalse(ApiSignHelper.timestampFresh(now - 301, tolerance), "过期请求应拒绝");
        assertFalse(ApiSignHelper.timestampFresh(now + 301, tolerance), "未来时间应拒绝（否则可签长期有效请求）");
    }

    @Test
    @DisplayName("appId / nonce 格式校验")
    void formatValidation() {
        assertTrue(ApiSignHelper.validAppId("partner-a_1"));
        assertFalse(ApiSignHelper.validAppId(""));
        assertFalse(ApiSignHelper.validAppId(null));
        assertFalse(ApiSignHelper.validAppId("bad app id"));

        assertTrue(ApiSignHelper.validNonce("abcd1234efgh"));
        assertFalse(ApiSignHelper.validNonce("short"), "过短容易被猜到");
        assertFalse(ApiSignHelper.validNonce("x".repeat(65)), "过长没有必要");
        assertFalse(ApiSignHelper.validNonce("bad nonce!"));
        assertFalse(ApiSignHelper.validNonce(null));
    }

    @Test
    @DisplayName("signRequest / verifyRequest 往返：签名通过，改任一要素即失败")
    void signAndVerifyRoundTrip() {
        EcdsaHelper.EcdsaKeyPair keyPair = EcdsaHelper.generateEcdsaKeys();
        String appId = "partner-a";
        String path = "/v2/partner/orders";
        long timestamp = Instant.now().getEpochSecond();
        String nonce = "abcd1234efgh";
        byte[] body = "{\"orderId\":1001}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        String signature = ApiSignHelper.signRequest(appId, "POST", path, timestamp, nonce,
                body, keyPair.getPrivateKey());

        assertTrue(ApiSignHelper.verifyRequest(appId, "POST", path, timestamp, nonce, body,
                signature, keyPair.getPublicKeyBase64()));
        // 篡改 body
        assertFalse(ApiSignHelper.verifyRequest(appId, "POST", path, timestamp, nonce,
                "{\"orderId\":9999}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                signature, keyPair.getPublicKeyBase64()));
        // 篡改路径 / 方法 / appId / 时间戳
        assertFalse(ApiSignHelper.verifyRequest(appId, "POST", "/v2/partner/other", timestamp, nonce, body,
                signature, keyPair.getPublicKeyBase64()));
        assertFalse(ApiSignHelper.verifyRequest(appId, "GET", path, timestamp, nonce, body,
                signature, keyPair.getPublicKeyBase64()));
        assertFalse(ApiSignHelper.verifyRequest("partner-b", "POST", path, timestamp, nonce, body,
                signature, keyPair.getPublicKeyBase64()));
        assertFalse(ApiSignHelper.verifyRequest(appId, "POST", path, timestamp + 1, nonce, body,
                signature, keyPair.getPublicKeyBase64()));
        // 换别人的公钥验签也失败
        assertFalse(ApiSignHelper.verifyRequest(appId, "POST", path, timestamp, nonce, body,
                signature, EcdsaHelper.generateEcdsaKeys().getPublicKeyBase64()));
    }
}
