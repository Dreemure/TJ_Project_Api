package com.example.tj_project_apicommon.Utils;

import com.example.tj_project_apicommon.Constants.Constant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.example.tj_project_apicommon.Utils.StringUtils.EMPTY;

/*
 * Web 层通用工具类。
 * 职责：封装 Servlet API 操作，提供请求/响应获取、请求头读取、参数拼接、判断请求来源（网关/Feign）等能力。
 * 使用：通过静态方法直接调用，适用于 Controller 层或拦截器。
 * 注意：依赖 RequestContextHolder（基于 ThreadLocal），需在 Servlet 线程内使用；所有方法均为空安全设计。
 */
@Slf4j
public class WebUtils {

    private WebUtils() {
        // 工具类私有构造，防止实例化
    }

    /**
     * 获取ServletRequestAttributes
     *
     * @return ServletRequestAttributes 可能为 null
     */
    public static ServletRequestAttributes getServletRequestAttributes() {
        var ra = RequestContextHolder.getRequestAttributes();
        return ra instanceof ServletRequestAttributes ? (ServletRequestAttributes) ra : null;
    }

    /**
     * 获取request
     *
     * @return HttpServletRequest 可能为 null
     */
    public static HttpServletRequest getRequest() {
        return Optional.ofNullable(getServletRequestAttributes())
                .map(ServletRequestAttributes::getRequest)
                .orElse(null);
    }

    /**
     * 获取response
     *
     * @return HttpServletResponse 可能为 null
     */
    public static HttpServletResponse getResponse() {
        return Optional.ofNullable(getServletRequestAttributes())
                .map(ServletRequestAttributes::getResponse)
                .orElse(null);
    }

    /**
     * 获取request header中的内容
     *
     * @param headerName 请求头名称
     * @return 请求头的值
     */
    public static String getHeader(String headerName) {
        var request = getRequest();
        return request == null ? null : request.getHeader(headerName);
    }

    /**
     * 设置响应头
     *
     * @param key   响应头键
     * @param value 响应头值
     */
    public static void setResponseHeader(String key, String value){
        var response = getResponse();
        if (response != null) {
            response.setHeader(key, value);
        }
    }

    /**
     * 获取请求中的 requestId（从请求头）
     *
     * @return requestId 或 null
     */
    public static String getRequestId() {
        return getHeader(Constant.REQUEST_ID_HEADER);
    }

    /**
     * 判断当前请求是否来自网关
     *
     * @return true 表示来自网关
     */
    public static boolean isGatewayRequest() {
        String originName = getHeader(Constant.REQUEST_FROM_HEADER);
        return Constant.GATEWAY_ORIGIN_NAME.equals(originName);
    }

    /**
     * 判断当前请求是否来自 Feign
     *
     * @return true 表示来自 Feign
     */
    public static boolean isFeignRequest() {
        String originName = getHeader(Constant.REQUEST_FROM_HEADER);
        return Constant.FEIGN_ORIGIN_NAME.equals(originName);
    }

    /**
     * 判断响应状态是否为成功（HTTP 状态码 < 300）
     *
     * @return true 表示成功
     */
    public static boolean isSuccess() {
        HttpServletResponse response = getResponse();
        return response != null && response.getStatus() < 300;
    }

    /**
     * 获取请求地址中的请求参数组装成 key1=value1&key2=value2
     * 如果key对应多个值，中间使用逗号隔开例如 key1对应value1，key2对应value2，value3， key1=value1&key2=value2,value3
     *
     * @param request
     * @return 返回拼接字符串
     */
    public static String getParameters(HttpServletRequest request) {
        var parameterMap = request.getParameterMap();
        return getParameters(parameterMap);
    }

    /**
     * 获取请求地址中的请求参数组装成 key1=value1&key2=value2
     * 如果key对应多个值，中间使用逗号隔开例如 key1对应value1，key2对应value2，value3， key1=value1&key2=value2,value3
     *
     * @param queries
     * @return
     */
    public  static <T> String getParameters(final Map<String, T> queries) {
        var buffer = new StringBuilder();
        for (var entry : queries.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            String joined;
            if (value instanceof String[] arr) {
                joined = String.join(",", arr);
            } else if (value instanceof Collection<?> coll) {
                joined = coll.stream().map(Object::toString).collect(Collectors.joining(","));
            } else {
                continue; // 不处理其他类型
            }
            buffer.append(key).append("=").append(joined).append("&");
        }
        return !buffer.isEmpty() ? buffer.substring(0, buffer.length() - 1) : EMPTY;
    }

    /**
     * 获取请求url中的uri
     *
     * @param url
     * @return
     */
    public static String getUri(String url){
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            var uri = URI.create(url);
            return uri.getPath();
        } catch (Exception e) {
            // fallback：手动截取（兼容非标准 URL）
            var uriPart = url;
            if (uriPart.startsWith("http://") || uriPart.startsWith("https://")) {
                var idx = uriPart.indexOf("/", 8); // 跳过协议
                if (idx != -1) {
                    uriPart = uriPart.substring(idx);
                }
            }
            var queryIdx = uriPart.indexOf('?');
            if (queryIdx != -1) {
                uriPart = uriPart.substring(0, queryIdx);
            }
            return uriPart;
        }
    }

    /**
     * 获取客户端真实 IP（通过 request.getRemoteAddr()）
     *
     * @return IP 地址，若无法获取则返回空字符串
     */
    public static String getRemoteAddr() {
        var request = getRequest();
        return request == null ? "" : request.getRemoteAddr();
    }

    /**
     * 创建 CookieBuilder 实例（链式构造 Cookie）
     *
     * @return CookieBuilder
     */
    public static CookieBuilder cookieBuilder(){
        return new CookieBuilder(getRequest(), getResponse());
    }
}
