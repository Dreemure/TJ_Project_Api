package com.tjxt.tjauth.Controller;

import com.tjxt.tjauth.Utils.EcdsaHelper;
import com.tjxt.tjauth.Utils.Sha3Helper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v2/key")
@Tag(name = "签名与哈希测试")
@RequiredArgsConstructor
public class KeysController {

    /**
     * 生成密钥对。返回：
     *  - pubKey: Base64(SPKI)  可发给前端 / 用于验签
     *  - priKey: Base64(PKCS#8) 仅测试用，生产绝不可返回
     */
    @GetMapping("/ecdsa/getkeyPair")
    public ResponseEntity<Map<String, Object>> getKeyPair() {
        EcdsaHelper.EcdsaKeyPair keyPair = EcdsaHelper.generateEcdsaKeys();

        Map<String, Object> response = new HashMap<>();
        response.put("publicKey", keyPair.getPublicKeyBase64());
        response.put("privateKey", keyPair.getPrivateKeyBase64());
        return ResponseEntity.ok(response);
    }

    /**
     * 用私钥签名。
     *
     * @param privateKeyBase64 Base64(PKCS#8) 私钥，来自 getkeyPair 返回的 priKey
     * @param t                待签名的原始字符串
     * @return Base64(P1363) 签名，可直接用 verifySignature 或前端 subtle.verify 验签
     */
    @GetMapping("/ecdsa/sign")
    public String sign(@RequestParam String privateKeyBase64,
                       @RequestParam String t) {
        PrivateKey privateKey = EcdsaHelper.importPrivateKey(privateKeyBase64);
        return EcdsaHelper.sign(t, privateKey);
    }

    /**
     * 用公钥验签。
     */
    @GetMapping("/ecdsa/verify")
    public boolean verify(@RequestParam String publicKeyBase64,
                          @RequestParam String signatureBase64,
                          @RequestParam String t) {
        return EcdsaHelper.verifySignature(publicKeyBase64, signatureBase64, t);
    }

    /**
     * 计算 SHA3-256，返回小写十六进制。
     */
    @GetMapping("/sha3/hash")
    public String computeSha3_256(@RequestParam String t) {
        return Sha3Helper.computeSha3_256(t);
    }
}