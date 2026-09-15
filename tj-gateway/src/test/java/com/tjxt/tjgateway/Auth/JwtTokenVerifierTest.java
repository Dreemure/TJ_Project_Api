package com.tjxt.tjgateway.Auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.tjxt.tjcommon.Constants.AuthConstants;
import com.tjxt.tjcommon.Constants.ErrorInfo;
import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import com.tjxt.tjcommon.Model.Response.R;
import com.tjxt.tjcommon.Utils.RoleUtils;
import com.tjxt.tjgateway.Config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * 网关 JWT 验签测试（不依赖 Spring 容器 / Nacos / Redis）。
 * 覆盖：合法 token、过期 token、被篡改 token、refresh token 误用、issuer 不匹配、公钥未就绪、角色映射。
 */
class JwtTokenVerifierTest {

    private static final String ISSUER = "tj-auth";

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private JwtTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
        this.verifier = newVerifier(publicKey, ISSUER);
    }

    @Test
    @DisplayName("合法 access token：解析出用户信息并映射为 ROLE_TEACHER")
    void verifyValidToken() {
        String token = sign(privateKey, ISSUER, Instant.now().plusSeconds(60),
                AuthConstants.TOKEN_TYPE_ACCESS, 3, "teacher");

        R<LoginUserDTO> result = verifier.verify(token);

        assertTrue(result.success(), result.getMsg());
        assertNotNull(result.getData());
        assertEquals(1001L, result.getData().getUserId());
        assertEquals(3L, result.getData().getRoleId());
        assertEquals(3, result.getData().getType().intValue());
        assertEquals("teacher", result.getData().getRoleName());
        assertEquals(Set.of(AuthConstants.ROLE_TEACHER), RoleUtils.resolve(result.getData()));
    }

    @Test
    @DisplayName("过期 token：返回 40101")
    void verifyExpiredToken() {
        String token = sign(privateKey, ISSUER, Instant.now().minusSeconds(60),
                AuthConstants.TOKEN_TYPE_ACCESS, 2, "student");

        R<LoginUserDTO> result = verifier.verify(token);

        assertFalse(result.success());
        assertEquals(ErrorInfo.Code.EXPIRED_TOKEN, result.getCode());
    }

    @Test
    @DisplayName("被其它密钥签发的 token：返回 40102")
    void verifyTamperedToken() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        RSAPrivateKey otherPrivateKey = (RSAPrivateKey) generator.generateKeyPair().getPrivate();
        String token = sign(otherPrivateKey, ISSUER, Instant.now().plusSeconds(60),
                AuthConstants.TOKEN_TYPE_ACCESS, 1, "admin");

        R<LoginUserDTO> result = verifier.verify(token);

        assertFalse(result.success());
        assertEquals(ErrorInfo.Code.INVALID_TOKEN, result.getCode());
    }

    @Test
    @DisplayName("refresh token 不能当 access token 使用")
    void verifyRefreshTokenRejected() {
        String token = sign(privateKey, ISSUER, Instant.now().plusSeconds(60),
                AuthConstants.TOKEN_TYPE_REFRESH, 1, "admin");

        R<LoginUserDTO> result = verifier.verify(token);

        assertFalse(result.success());
        assertEquals(ErrorInfo.Code.INVALID_TOKEN, result.getCode());
    }

    @Test
    @DisplayName("issuer 不匹配：返回 40102")
    void verifyIssuerMismatch() {
        String token = sign(privateKey, "other-issuer", Instant.now().plusSeconds(60),
                AuthConstants.TOKEN_TYPE_ACCESS, 1, "admin");

        R<LoginUserDTO> result = verifier.verify(token);

        assertFalse(result.success());
        assertEquals(ErrorInfo.Code.INVALID_TOKEN, result.getCode());
    }

    @Test
    @DisplayName("空 token：返回 40102")
    void verifyBlankToken() {
        assertEquals(ErrorInfo.Code.INVALID_TOKEN, verifier.verify(null).getCode());
        assertEquals(ErrorInfo.Code.INVALID_TOKEN, verifier.verify("  ").getCode());
    }

    @Test
    @DisplayName("公钥尚未加载（auth 服务不可用）：返回 40100，避免被当成无效 token")
    void verifyWithoutPublicKey() {
        JwtTokenVerifier noKeyVerifier = newVerifier(null, ISSUER);

        R<LoginUserDTO> result = noKeyVerifier.verify(sign(privateKey, ISSUER, Instant.now().plusSeconds(60),
                AuthConstants.TOKEN_TYPE_ACCESS, 1, "admin"));

        assertFalse(result.success());
        assertEquals(ErrorInfo.Code.UNAUTHORIZED, result.getCode());
    }

    // ==================== 测试辅助 ====================

    /**
     * 构造只使用指定公钥的验签器（跳过 JWKS 拉取）。
     */
    private JwtTokenVerifier newVerifier(RSAPublicKey key, String issuer) {
        JwksPublicKeyProvider provider = new JwksPublicKeyProvider(new AuthProperties(), null, null) {
            @Override
            public RSAPublicKey getPublicKey() {
                return key;
            }

            @Override
            public boolean refreshNow() {
                return false;
            }
        };
        AuthProperties properties = new AuthProperties();
        properties.getJwt().setIssuer(issuer);
        return new JwtTokenVerifier(provider, properties);
    }

    private String sign(RSAPrivateKey signKey, String issuer, Instant expiresAt,
                        String tokenType, Integer type, String roleName) {
        return JWT.create()
                .withIssuer(issuer)
                .withClaim(AuthConstants.CLAIM_USER_ID, 1001L)
                .withClaim(AuthConstants.CLAIM_ROLE_ID, 3L)
                .withClaim(AuthConstants.CLAIM_ROLE_NAME, roleName)
                .withClaim(AuthConstants.CLAIM_TYPE, type)
                .withClaim(AuthConstants.CLAIM_TOKEN_TYPE, tokenType)
                .withExpiresAt(Date.from(expiresAt))
                .sign(Algorithm.RSA256(publicKey, signKey));
    }
}
