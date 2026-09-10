package com.example.tj_project_apicommon.Utils;

import com.ijpay.core.kit.WxPayKit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;


/*
 * 签名工具类（适配 IJPay 聚合支付）。
 * 职责：封装微信支付（V2/V3）与支付宝的签名及验签逻辑。
 * 说明：
 *   - 微信 V2：MD5 或 HMAC-SHA256，密钥为 APIv2 密钥。
 *   - 微信 V3：SHA256 with RSA，使用商户 API 证书私钥签名，使用微信平台证书公钥验签。
 *   - 支付宝：RSA2（SHA256 with RSA），使用应用私钥签名，支付宝公钥验签。
 * 注意：V3 签名结果需 Base64 编码，验签时需确保传入微信平台证书（公钥）内容。
 */
@Slf4j
public class SignUtils {
    private SignUtils() {}

    // ================= 微信支付 (V2) =================

    /**
     * 生成微信支付 V2 签名。
     *
     * @param params   参与签名的参数（不含 sign 字段）
     * @param apiKey   商户 APIv2 密钥
     * @param signType 签名类型: MD5 或 HMAC-SHA256
     * @return 签名结果（大写）
     */
    public static String signForWxPay(Map<String, String> params, String apiKey, String signType) {
        String signContent = buildWxSignContent(params);
        String sign;
        if ("MD5".equalsIgnoreCase(signType)) {
            sign = WxPayKit.md5(signContent + "&key=" + apiKey).toUpperCase();
        } else if ("HMAC-SHA256".equalsIgnoreCase(signType)) {
            sign = WxPayKit.hmacSha256(signContent, apiKey).toUpperCase();
        } else {
            throw new IllegalArgumentException("不支持的微信签名类型: " + signType);
        }
        return sign;
    }

    /**
     * 验证微信支付 V2 签名。
     *
     * @param params   包含 sign 字段的完整参数
     * @param apiKey   商户 APIv2 密钥
     * @param signType 签名类型
     * @return 验证是否通过
     */
    public static boolean verifySignForWxPay(Map<String, String> params, String apiKey, String signType) {
        String signFromRequest = params.get("sign");
        if (StringUtils.isEmpty(signFromRequest)) {
            log.warn("微信 V2 签名验证失败: 签名字段为空");
            return false;
        }
        Map<String, String> paramsToSign = new TreeMap<>(params);
        paramsToSign.remove("sign");
        String calculatedSign = signForWxPay(paramsToSign, apiKey, signType);
        boolean result = calculatedSign.equalsIgnoreCase(signFromRequest);
        if (!result) {
            log.warn("微信 V2 签名验证失败: 计算值[{}], 请求值[{}]", calculatedSign, signFromRequest);
        }
        return result;
    }

    /**
     * 构建微信 V2 签名待签名字符串：参数按字典序排列，过滤空值，拼接为 key=value&... 格式。
     */
    private static String buildWxSignContent(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }


    // ================= 微信支付 (V3) =================

    /**
     * 生成微信支付 V3 签名（SHA256 with RSA）。
     * <p>签名串格式：HTTP请求方法\nURL路径\n时间戳\n随机字符串\n请求报文主体\n
     *
     * @param method       HTTP 请求方法（GET/POST 等）
     * @param urlPath      请求 URL 的路径部分（如 /v3/pay/transactions/jsapi）
     * @param timestamp    当前时间戳（秒）
     * @param nonceStr     随机字符串
     * @param body         请求报文主体（GET 请求可为空字符串）
     * @param privateKey   商户 API 证书私钥（PrivateKey 对象）
     * @return Base64 编码的签名值
     */
    public static String signForWxPayV3(String method, String urlPath, long timestamp,
                                        String nonceStr, String body, PrivateKey privateKey) {
        // 构建待签名串
        String signContent = buildWxV3SignContent(method, urlPath, timestamp, nonceStr, body);
        return rsaSign(signContent, privateKey);
    }

    /**
     * 验证微信支付 V3 签名（使用微信平台证书公钥）。
     *
     * @param method      HTTP 请求方法
     * @param urlPath     请求路径
     * @param timestamp   时间戳
     * @param nonceStr    随机字符串
     * @param body        响应体或回调报文主体
     * @param signature   Base64 编码的签名值（来自 Wechatpay-Signature 头）
     * @param publicKey   微信平台证书公钥（PublicKey 对象）
     * @return 验证是否通过
     */
    public static boolean verifySignForWxPayV3(String method, String urlPath, long timestamp,
                                               String nonceStr, String body,
                                               String signature, PublicKey publicKey) {
        String signContent = buildWxV3SignContent(method, urlPath, timestamp, nonceStr, body);
        return rsaVerify(signContent, signature, publicKey);
    }

    /**
     * 构建微信支付 V3 待签名串。
     * <p>格式：method\nurlPath\ntimestamp\nnonceStr\nbody\n
     */
    private static String buildWxV3SignContent(String method, String urlPath,
                                               long timestamp, String nonceStr, String body) {
        return method + "\n" + urlPath + "\n" + timestamp + "\n" + nonceStr + "\n" + body + "\n";
    }

    /**
     * 使用私钥进行 SHA256 with RSA 签名，返回 Base64 结果。
     */
    private static String rsaSign(String content, PrivateKey privateKey) {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(privateKey);
            sign.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sign.sign());
        } catch (Exception e) {
            log.error("微信 V3 签名生成失败", e);
            throw new RuntimeException("微信 V3 签名生成失败", e);
        }
    }

    /**
     * 使用公钥进行 SHA256 with RSA 验签。
     */
    private static boolean rsaVerify(String content, String signatureBase64, PublicKey publicKey) {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(publicKey);
            sign.update(content.getBytes(StandardCharsets.UTF_8));
            return sign.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            log.error("微信 V3 验签失败", e);
            return false;
        }
    }


    // ================= 支付宝 =================

    /**
     * 生成支付宝签名（RSA2）。
     *
     * @param content    待签名内容（通常是排序后的参数拼接串）
     * @param privateKey 应用私钥
     * @param signType   签名类型（如 RSA2）
     * @return Base64 编码的签名结果
     */
    public static String signForAliPay(String content, String privateKey, String signType) {
        try {
            return com.alipay.api.internal.util.AlipaySignature.rsaSign(
                    content, privateKey, "UTF-8", signType);
        } catch (Exception e) {
            log.error("支付宝签名生成失败", e);
            throw new RuntimeException("支付宝签名生成失败", e);
        }
    }

    /**
     * 验证支付宝签名。
     *
     * @param content        待验签内容
     * @param sign           签名值（Base64）
     * @param aliPayPublicKey 支付宝公钥
     * @param signType       签名类型
     * @return 验证是否通过
     */
    public static boolean verifySignForAliPay(String content, String sign, String aliPayPublicKey, String signType) {
        try {
            return com.alipay.api.internal.util.AlipaySignature.rsaCheck(
                    content, sign, aliPayPublicKey, "UTF-8", signType);
        } catch (Exception e) {
            log.error("支付宝签名验证失败", e);
            return false;
        }
    }

    /**
     * 构建支付宝待签名字符串：参数按字典序排列，过滤空值，拼接为 key=value&... 格式。
     */
    public static String buildAliPaySignContent(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }
}
