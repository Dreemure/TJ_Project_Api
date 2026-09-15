package com.example.tjauth.Utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.tjauth.Config.AuthProperties;
import com.example.tjauth.Constants.AuthConstants;
import com.example.tjcommon.Model.Dto.LoginUserDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/*
 * JWT 签发与校验器（RS256）。
 * 职责：
 *   1. 用私钥签发 access token / refresh token
 *   2. 用公钥校验 refresh token
 *   3. 把 refresh token 的 JTI 存 Redis（用于登出、单点控制）
 * 说明：与 AuthUtils 互补——本类签发 + 校验 refresh，AuthUtils 校验 access。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtIssuer {

    private final AuthProperties authProperties;
    private final RsaKeyLoader rsaKeyLoader;
    private final StringRedisTemplate stringRedisTemplate;

    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    public void init() {
        // 加载私钥 + 公钥（签发用私钥，校验用公钥）
        PrivateKey privateKey = rsaKeyLoader.loadPrivateKey(
                authProperties.getJwt().getPrivateKeyPath());
        PublicKey publicKey = rsaKeyLoader.loadPublicKey(
                authProperties.getJwt().getPublicKeyPath());

        this.algorithm = Algorithm.RSA256((RSAPublicKey) publicKey, (RSAPrivateKey) privateKey);
        this.verifier = JWT.require(algorithm)
                .withIssuer(authProperties.getJwt().getIssuer())
                .build();
        log.info("JWT 签发器初始化完成，算法：RS256");
    }

    // ==================== 签发 ====================

    /**
     * 签发 access token。
     *
     * @param user 登录用户信息（含 userId / roleId / roleName）
     * @return JWT 字符串
     */
    public String issueToken(LoginUserDTO user) {
        Date expiresAt = new Date(System.currentTimeMillis()
                + authProperties.getJwt().getTokenTtl().toMillis());
        return JWT.create()
                .withIssuer(authProperties.getJwt().getIssuer())
                .withClaim("userId", user.getUserId())
                .withClaim("roleId", user.getRoleId())
                .withClaim("roleName", user.getRoleName())
                .withExpiresAt(expiresAt)
                .withIssuedAt(new Date())
                .sign(algorithm);
    }

    /**
     * 签发 refresh token，同时把 JTI 存 Redis（用于登出、单点控制）。
     *
     * @param user 登录用户信息（含 userId）
     * @return JWT 字符串
     */
    public String issueRefreshToken(LoginUserDTO user) {
        Date expiresAt = new Date(System.currentTimeMillis()
                + authProperties.getJwt().getRefreshTtl().toMillis());
        String jti = UUID.randomUUID().toString();

        String token = JWT.create()
                .withIssuer(authProperties.getJwt().getIssuer())
                .withJWTId(jti)
                .withClaim("userId", user.getUserId())
                .withClaim("roleId", user.getRoleId())
                .withClaim("roleName", user.getRoleName())
                .withClaim("type", "refresh")
                .withExpiresAt(expiresAt)
                .withIssuedAt(new Date())
                .sign(algorithm);

        // 把 JTI 存入 Redis，TTL 与 token 一致
        stringRedisTemplate.opsForValue().set(
                AuthConstants.JWT_REDIS_KEY_PREFIX + jti,
                String.valueOf(user.getUserId()),
                authProperties.getJwt().getRefreshTtl().toMillis(),
                TimeUnit.MILLISECONDS);

        return token;
    }

    // ==================== 校验 ====================

    /**
     * 解析并校验 refresh token。
     * <p>校验项：签名、过期时间、type=refresh、Redis 中 JTI 是否存在（登出后会被删）。
     *
     * @param refreshToken refresh token 字符串
     * @return LoginUserDTO；校验失败返回 null
     */
    public LoginUserDTO parseRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return null;
        }
        try {
            DecodedJWT jwt = verifier.verify(refreshToken);

            // 1. 校验 token 类型
            if (!"refresh".equals(jwt.getClaim("type").asString())) {
                log.debug("token 类型不是 refresh");
                return null;
            }

            // 2. 校验 Redis 中的 JTI 是否存在（登出后会被删除）
            String jti = jwt.getId();
            if (!StringUtils.hasText(jti)
                    || !Boolean.TRUE.equals(stringRedisTemplate.hasKey(
                    AuthConstants.JWT_REDIS_KEY_PREFIX + jti))) {
                log.debug("refresh token 已失效（JTI 不存在）");
                return null;
            }

            // 3. 组装用户信息
            var user = new LoginUserDTO();
            user.setUserId(jwt.getClaim("userId").asLong());
            user.setRoleId(jwt.getClaim("roleId").asLong());
            user.setRoleName(jwt.getClaim("roleName").asString());
            return user;
        } catch (TokenExpiredException e) {
            log.debug("refresh token 已过期: {}", e.getMessage());
            return null;
        } catch (JWTVerificationException e) {
            log.debug("refresh token 校验失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 清理 ====================

    /**
     * 清理 refresh token（删除 Redis 中的 JTI）。
     * <p>只解码不验签——即使 token 已过期，也能从 payload 里读出 JTI 去清理。
     *
     * @param refreshToken refresh token 字符串
     */
    public void cleanRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        try {
            DecodedJWT jwt = JWT.decode(refreshToken);   // 只解码，不验签
            String jti = jwt.getId();
            if (StringUtils.hasText(jti)) {
                stringRedisTemplate.delete(AuthConstants.JWT_REDIS_KEY_PREFIX + jti);
                log.debug("已清理 refresh token JTI: {}", jti);
            }
        } catch (Exception e) {
            log.warn("清理 refresh token 失败: {}", e.getMessage());
        }
    }
}