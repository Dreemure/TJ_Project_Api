package com.tjxt.tjgateway.Auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.tjxt.tjcommon.Constants.AuthConstants;
import com.tjxt.tjcommon.Constants.ErrorInfo;
import com.tjxt.tjcommon.Model.Dto.LoginUserDTO;
import com.tjxt.tjcommon.Model.Response.R;
import com.tjxt.tjgateway.Config.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.interfaces.RSAPublicKey;

/*
 * JWT 验签器（RS256）。
 * 职责：用 auth 服务颁发的公钥校验 access token，并把 JWT 声明翻译成 LoginUserDTO。
 * 说明：
 *   - 与 auth 服务共用 java-jwt + RS256，声明名称取自 AuthConstants（跨模块协议常量）
 *   - 验签失败且疑似密钥轮换时，主动刷新一次公钥后重试（带节流，见 JwksPublicKeyProvider#refreshNow）
 *   - 返回值统一用 R 包装：成功带 LoginUserDTO，失败带 40100/40101/40102 业务码，与原项目 AuthUtil#parseToken 语义一致
 */
@Slf4j
@Component
public class JwtTokenVerifier {

    private final JwksPublicKeyProvider keyProvider;
    private final AuthProperties authProperties;

    /** 与 boundKey 绑定的验签器，公钥变化时重建 */
    private volatile JWTVerifier verifier;
    private volatile RSAPublicKey boundKey;

    public JwtTokenVerifier(JwksPublicKeyProvider keyProvider, AuthProperties authProperties) {
        this.keyProvider = keyProvider;
        this.authProperties = authProperties;
    }

    /**
     * 校验 token 并解析登录用户信息。
     *
     * @param token access token（不含 Bearer 前缀）
     * @return R&lt;LoginUserDTO&gt;：成功为 ok(data)，失败为 40100/40101/40102
     */
    public R<LoginUserDTO> verify(String token) {
        if (!StringUtils.hasText(token)) {
            return R.error(ErrorInfo.Code.INVALID_TOKEN, ErrorInfo.Msg.INVALID_TOKEN);
        }
        JWTVerifier jwtVerifier = verifier();
        if (jwtVerifier == null) {
            // 公钥尚未加载完成（auth 服务未启动/服务发现不可用）
            log.warn("JWT 公钥尚未就绪，无法验签");
            return R.error(ErrorInfo.Code.UNAUTHORIZED, ErrorInfo.Msg.AUTH_SERVICE_UNAVAILABLE);
        }
        try {
            return toResult(jwtVerifier.verify(token));
        } catch (TokenExpiredException e) {
            return R.error(ErrorInfo.Code.EXPIRED_TOKEN, ErrorInfo.Msg.EXPIRED_TOKEN);
        } catch (JWTVerificationException e) {
            // 签名不匹配可能是 auth 服务轮换了密钥：刷新一次后重试
            if (keyProvider.refreshNow()) {
                JWTVerifier refreshed = verifier();
                if (refreshed != null) {
                    try {
                        return toResult(refreshed.verify(token));
                    } catch (JWTVerificationException ignored) {
                        // 刷新后仍然失败，按无效 token 处理
                    }
                }
            }
            log.debug("token 校验失败：{}", e.getMessage());
            return R.error(ErrorInfo.Code.INVALID_TOKEN, ErrorInfo.Msg.INVALID_TOKEN);
        } catch (Exception e) {
            log.warn("token 解析异常：{}", e.getMessage());
            return R.error(ErrorInfo.Code.INVALID_TOKEN, ErrorInfo.Msg.INVALID_TOKEN);
        }
    }

    /**
     * 获取（必要时重建）验签器：公钥实例变化时重建，避免每次请求都构造。
     */
    private JWTVerifier verifier() {
        RSAPublicKey key = keyProvider.getPublicKey();
        if (key == null) {
            return null;
        }
        JWTVerifier current = this.verifier;
        if (current != null && key == this.boundKey) {
            return current;
        }
        synchronized (this) {
            if (this.verifier == null || key != this.boundKey) {
                var builder = JWT.require(Algorithm.RSA256(key, null));
                String issuer = authProperties.getJwt().getIssuer();
                if (StringUtils.hasText(issuer)) {
                    builder.withIssuer(issuer);
                }
                this.verifier = builder.build();
                this.boundKey = key;
                log.info("网关 JWT 验签器已（重）建完成，issuer 校验：{}",
                        StringUtils.hasText(issuer) ? issuer : "未开启");
            }
            return this.verifier;
        }
    }

    /**
     * 把解码后的 JWT 翻译为登录用户信息。
     */
    private R<LoginUserDTO> toResult(DecodedJWT jwt) {
        // refresh token 不能当 access token 使用（有效期更长，只能用于换取新的 access token）
        String tokenType = jwt.getClaim(AuthConstants.CLAIM_TOKEN_TYPE).asString();
        if (StringUtils.hasText(tokenType) && !AuthConstants.TOKEN_TYPE_ACCESS.equals(tokenType)) {
            log.debug("token 类型不是 access：{}", tokenType);
            return R.error(ErrorInfo.Code.INVALID_TOKEN, ErrorInfo.Msg.INVALID_TOKEN);
        }
        var user = new LoginUserDTO();
        user.setUserId(jwt.getClaim(AuthConstants.CLAIM_USER_ID).asLong());
        if (user.getUserId() == null) {
            return R.error(ErrorInfo.Code.INVALID_TOKEN, ErrorInfo.Msg.INVALID_TOKEN);
        }
        user.setRoleId(jwt.getClaim(AuthConstants.CLAIM_ROLE_ID).asLong());
        user.setRoleName(jwt.getClaim(AuthConstants.CLAIM_ROLE_NAME).asString());
        user.setType(readType(jwt));
        return R.ok(user);
    }

    /**
     * 读取用户类型声明；类型缺失或格式非法时不报错（仅导致下游无角色权限）。
     */
    private Integer readType(DecodedJWT jwt) {
        try {
            return jwt.getClaim(AuthConstants.CLAIM_TYPE).asInt();
        } catch (Exception e) {
            log.debug("token 中 {} 声明格式非法：{}", AuthConstants.CLAIM_TYPE, e.getMessage());
            return null;
        }
    }
}
