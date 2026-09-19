package com.tjxt.tjauth.Controller;

import com.tjxt.tjauth.Config.AuthProperties;
import com.tjxt.tjauth.Utils.RsaKeyLoader;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;

/*
 * JWK 公钥端点。
 * 职责：暴露 RSA 公钥的标准 JWK 格式，供其他服务拉取用于 JWT 验签（RS256）。
 * 说明：
 *   - 遵循 RFC 7517（JWK）标准，返回 {keys: [...]} 结构
 *   - 其他服务可用 nimbus-jose-jwt 的 JWK.parse() 解析后构造 RSA 公钥
 *   - 生产环境可加 clientId / clientSecret 校验，防止公钥被任意拉取
 */
@Hidden  // 不在 Swagger 文档中显示
@RestController
@RequestMapping("/v2/jwks")
@RequiredArgsConstructor
public class JwkController {

    private final AuthProperties authProperties;
    private final RsaKeyLoader rsaKeyLoader;

    /**
     * 获取 RSA 公钥（JWK 格式）。
     *
     * @return {keys: [{kty: "RSA", n: "...", e: "...", alg: "RS256", use: "sig"}]}
     */
    @GetMapping
    public Map<String, Object> getJwk() {
        // 1. 加载公钥
        PublicKey publicKey = rsaKeyLoader.loadPublicKey(
                authProperties.getJwt().getPublicKeyPath());
        RSAPublicKey rsa = (RSAPublicKey) publicKey;

        // 2. 构造 JWK（Base64URL 编码，无 padding）
        var encoder = Base64.getUrlEncoder().withoutPadding();
        Map<String, Object> jwk = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "n", encoder.encodeToString(stripLeadingZero(rsa.getModulus().toByteArray())),
                "e", encoder.encodeToString(stripLeadingZero(rsa.getPublicExponent().toByteArray()))
        );

        // 3. 包成标准 JWK Set 格式
        return Map.of("keys", new Object[]{jwk});
    }

    /**
     * 去掉 BigInteger.toByteArray() 可能产生的前导 0。
     * <p>BigInteger 转 byte[] 时，若最高位是 1（会被解析为负数），会在前面补一个 0x00。
     * <p>JWK 的 n/e 字段需要的是无符号大整数的 Base64URL 编码，所以要剥掉这个 0x00。
     */
    private byte[] stripLeadingZero(byte[] bytes) {
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] result = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, result, 0, result.length);
            return result;
        }
        return bytes;
    }
}