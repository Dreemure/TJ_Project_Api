package com.tjxt.tjcommon.Sign;

import com.tjxt.tjcommon.Constants.AuthConstants;
import com.tjxt.tjcommon.Utils.ApiSignHelper;
import com.tjxt.tjcommon.Utils.EcdsaHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * 请求签名过滤器端到端测试（真实 ECDSA 密钥 + 真实 HTTP 请求对象，不启动 Spring 容器）。
 * 覆盖：正常放行、篡改请求体/路径、时间戳过期、重放、未配置的 appId、未保护路径直接放行，
 *       以及"通过后业务侧仍能读到请求体"。
 */
@DisplayName("请求签名校验过滤器")
class ApiSignatureFilterTest {

    private static final String APP_ID = "partner-a";
    private static final String PATH = "/v2/partner/orders";
    private static final String BODY = "{\"orderId\":1001,\"amount\":99.00}";

    private EcdsaHelper.EcdsaKeyPair partnerKeyPair;
    private ApiSignProperties properties;
    private ApiSignatureFilter filter;
    private InMemoryNonceStore nonceStore;

    @BeforeEach
    void setUp() {
        partnerKeyPair = EcdsaHelper.generateEcdsaKeys();
        properties = new ApiSignProperties();
        properties.setEnabled(true);
        properties.setIncludePath(java.util.Set.of("/v2/partner/**"));
        properties.setTolerance(Duration.ofMinutes(5));
        properties.setNonceTtl(Duration.ofMinutes(10));
        properties.getApps().put(APP_ID, appKey(partnerKeyPair.getPublicKeyBase64()));
        nonceStore = new InMemoryNonceStore();
        filter = new ApiSignatureFilter(properties, nonceStore, new DefaultResourceLoader());
    }

    @Test
    @DisplayName("签名正确：放行，且下游仍能读到请求体")
    void validSignaturePassesThrough() throws Exception {
        MockHttpServletRequest request = signedRequest(BODY, Instant.now().getEpochSecond(), newNonce());
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus(), "合法请求不应被拦截");
        assertTrue(chain.invoked, "合法请求应继续往下走");
        // 过滤器读过 body，包装后下游必须还能读
        assertEquals(BODY, chain.body);
        assertEquals(APP_ID, request.getAttribute(ApiSignHelper.ATTR_APP_ID), "应把 appId 交给业务侧");
    }

    @Test
    @DisplayName("篡改请求体：拒绝（401）")
    void tamperedBodyIsRejected() throws Exception {
        MockHttpServletRequest request = signedRequest(BODY, Instant.now().getEpochSecond(), newNonce());
        request.setContent("{\"orderId\":1001,\"amount\":0.01}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new RecordingChain());

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString(StandardCharsets.UTF_8).contains("签名校验未通过"));
    }

    @Test
    @DisplayName("篡改路径：拒绝（401）")
    void tamperedPathIsRejected() throws Exception {
        MockHttpServletRequest request = signedRequest(BODY, Instant.now().getEpochSecond(), newNonce());
        request.setRequestURI("/v2/partner/other");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new RecordingChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("时间戳过期：拒绝（401）")
    void expiredTimestampIsRejected() throws Exception {
        long old = Instant.now().minus(Duration.ofMinutes(30)).getEpochSecond();
        MockHttpServletRequest request = signedRequest(BODY, old, newNonce());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new RecordingChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("重放同一个请求：第二次拒绝（nonce 已被消费）")
    void replayIsRejected() throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String nonce = newNonce();
        MockHttpServletRequest first = signedRequest(BODY, timestamp, nonce);
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(first, firstResponse, new RecordingChain());
        assertEquals(200, firstResponse.getStatus());

        MockHttpServletRequest second = signedRequest(BODY, timestamp, nonce);
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(second, secondResponse, new RecordingChain());
        assertEquals(401, secondResponse.getStatus(), "同一 nonce 再放行就等于没有防重放");
    }

    @Test
    @DisplayName("签名失败时不消费 nonce：用合法签名重试同一 nonce 仍可通过")
    void failedSignatureDoesNotBurnNonce() throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String nonce = newNonce();
        MockHttpServletRequest bad = signedRequest(BODY, timestamp, nonce);
        bad.setContent("{\"orderId\":1001,\"amount\":0.01}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse badResponse = new MockHttpServletResponse();
        filter.doFilter(bad, badResponse, new RecordingChain());
        assertEquals(401, badResponse.getStatus());

        MockHttpServletRequest good = signedRequest(BODY, timestamp, nonce);
        MockHttpServletResponse goodResponse = new MockHttpServletResponse();
        filter.doFilter(good, goodResponse, new RecordingChain());
        assertEquals(200, goodResponse.getStatus(), "验签失败不应消耗 nonce（否则攻击者可刷掉合法 nonce）");
    }

    @Test
    @DisplayName("未配置的 appId / 缺签名头：拒绝（401）")
    void unknownAppIdAndMissingHeadersAreRejected() throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String nonce = newNonce();
        byte[] body = BODY.getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest unknown = new MockHttpServletRequest("POST", PATH);
        unknown.setContent(body);
        unknown.addHeader(AuthConstants.SIGN_APP_ID_HEADER, "not-configured");
        unknown.addHeader(AuthConstants.SIGN_TIMESTAMP_HEADER, String.valueOf(timestamp));
        unknown.addHeader(AuthConstants.SIGN_NONCE_HEADER, nonce);
        unknown.addHeader(AuthConstants.SIGN_SIGNATURE_HEADER, ApiSignHelper.signRequest(
                APP_ID, "POST", PATH, timestamp, nonce, body, partnerKeyPair.getPrivateKey()));
        MockHttpServletResponse unknownResponse = new MockHttpServletResponse();
        filter.doFilter(unknown, unknownResponse, new RecordingChain());
        assertEquals(401, unknownResponse.getStatus());

        MockHttpServletRequest noHeaders = new MockHttpServletRequest("POST", PATH);
        noHeaders.setContent(body);
        MockHttpServletResponse noHeadersResponse = new MockHttpServletResponse();
        filter.doFilter(noHeaders, noHeadersResponse, new RecordingChain());
        assertEquals(401, noHeadersResponse.getStatus());
    }

    @Test
    @DisplayName("未命中保护路径：不校验直接放行（对现有接口零影响）")
    void unprotectedPathPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v2/courses/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(chain.invoked, "请求应继续往下走");
    }

    @Test
    @DisplayName("用别人的私钥签名：拒绝（公钥必须服务端固定）")
    void signatureFromAnotherKeyIsRejected() throws Exception {
        EcdsaHelper.EcdsaKeyPair attacker = EcdsaHelper.generateEcdsaKeys();
        long timestamp = Instant.now().getEpochSecond();
        String nonce = newNonce();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", PATH);
        request.setContent(BODY.getBytes(StandardCharsets.UTF_8));
        request.addHeader(AuthConstants.SIGN_APP_ID_HEADER, APP_ID);
        request.addHeader(AuthConstants.SIGN_TIMESTAMP_HEADER, String.valueOf(timestamp));
        request.addHeader(AuthConstants.SIGN_NONCE_HEADER, nonce);
        request.addHeader(AuthConstants.SIGN_SIGNATURE_HEADER,
                ApiSignHelper.signRequest(APP_ID, "POST", PATH, timestamp, nonce,
                        BODY.getBytes(StandardCharsets.UTF_8), attacker.getPrivateKey()));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new RecordingChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("超过 body 上限：拒绝（不做无上限缓冲）")
    void oversizedBodyIsRejected() throws Exception {
        properties.setMaxBodyBytes(16);
        String bigBody = "x".repeat(64);
        MockHttpServletRequest request = signedRequest(bigBody, Instant.now().getEpochSecond(), newNonce());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new RecordingChain());

        assertEquals(401, response.getStatus());
    }

    // ==================== 测试辅助 ====================

    private static ApiSignProperties.AppKey appKey(String publicKeyBase64) {
        ApiSignProperties.AppKey key = new ApiSignProperties.AppKey();
        key.setPublicKey(publicKeyBase64);
        key.setRemark("单元测试用");
        return key;
    }

    private String newNonce() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 构造一个带完整签名头的请求 */
    private MockHttpServletRequest signedRequest(String body, long timestamp, String nonce) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", PATH);
        request.setRequestURI(PATH);
        byte[] bodyBytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        request.setContent(bodyBytes);
        request.addHeader(AuthConstants.SIGN_APP_ID_HEADER, APP_ID);
        request.addHeader(AuthConstants.SIGN_TIMESTAMP_HEADER, String.valueOf(timestamp));
        request.addHeader(AuthConstants.SIGN_NONCE_HEADER, nonce);
        request.addHeader(AuthConstants.SIGN_SIGNATURE_HEADER,
                ApiSignHelper.signRequest(APP_ID, "POST", PATH, timestamp, nonce, bodyBytes, partnerKeyPair.getPrivateKey()));
        return request;
    }

    /** 记录下游读到的请求体：用来验证"过滤器读过 body 后，下游（Controller）仍能读到" */
    static class RecordingChain implements FilterChain {
        private String body;
        private boolean invoked;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException {
            invoked = true;
            body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
