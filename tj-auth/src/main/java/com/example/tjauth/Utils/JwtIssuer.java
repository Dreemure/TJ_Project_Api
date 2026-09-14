package com.example.tjauth.Utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.tjauth.Config.AuthProperties;
import com.example.tjcommon.Model.Dto.LoginUserDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;

/*
 * JWT 签发器（RS256）。
 * 职责：用 RSA 私钥签发 access token / refresh token。
 * 说明：与 AuthUtils 互补——本类签发，AuthUtils 验签。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtIssuer {

    private final AuthProperties authProperties;
    private final RsaKeyLoader rsaKeyLoader;

    private Algorithm algorithm;

    @PostConstruct
    public void init() {
        // 加载私钥
        PrivateKey privateKey = rsaKeyLoader.loadPrivateKey(
                authProperties.getJwt().getPrivateKeyPath());
        this.algorithm = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);
        log.info("JWT 签发器初始化完成，算法：RS256");
    }

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
                .withClaim("roleName", user.getRoleName())   // ← 关键：带上 roleName
                .withExpiresAt(expiresAt)
                .withIssuedAt(new Date())
                .sign(algorithm);
    }

    /**
     * 签发 refresh token。
     *
     * @param userId 用户id
     * @return JWT 字符串
     */
    public String issueRefreshToken(Long userId) {
        Date expiresAt = new Date(System.currentTimeMillis()
                + authProperties.getJwt().getRefreshTtl().toMillis());
        return JWT.create()
                .withIssuer(authProperties.getJwt().getIssuer())
                .withClaim("userId", userId)
                .withClaim("type", "refresh")
                .withExpiresAt(expiresAt)
                .withIssuedAt(new Date())
                .sign(algorithm);
    }
}