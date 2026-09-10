package com.example.tj_project_apicommon.Utils;

import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * 请求参数处理工具类。
 * 职责：对 URL 查询字符串进行解码、按参数名升序排序，并重新拼接为 key1=value1&key2=value2 格式。
 * 使用：常用于签名计算等需要参数有序的场景，如 RequestUtils.toSortQueryParams("b=2&a=1")。
 * 注意：参数值经过 UTF-8 URL 解码，排序基于解码后的完整键值对字符串。
 */
public class RequestUtils {

    public static final String UTF8_ENC = StandardCharsets.UTF_8.name();

    private RequestUtils() {}

    /**
     * 将请求参数进行升序排序，重新组装。
     * <p>例如输入 "b=2&a=1"，输出 "a=1&b=2"。
     *
     * @param originQueryParam 原始请求参数（形如 a=1&b=2）
     * @return 排序并重新拼接后的参数字符串；若输入为空则返回空字符串
     */
    public static String toSortQueryParams(String originQueryParam) {
        if (originQueryParam == null || originQueryParam.isEmpty()) {
            return "";
        }

        var queryParams = new ArrayList<String>();
        for (var kv : originQueryParam.split("&")) {
            var t = kv.split("=");
            var key = UriUtils.decode(t[0], StandardCharsets.UTF_8);
            var value = t.length > 1 ? UriUtils.decode(t[1], StandardCharsets.UTF_8) : "";
            queryParams.add(key + "=" + value);
        }

        Collections.sort(queryParams);

        var buffer = new StringBuilder();
        for (var param : queryParams) {
            buffer.append(param).append("&");
        }

        return !buffer.isEmpty() ? buffer.substring(0, buffer.length() - 1) : "";
    }
}
