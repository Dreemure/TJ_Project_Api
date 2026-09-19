package com.tjxt.tjcommon.Sign;

import com.tjxt.tjcommon.Constants.AuthConstants;
import com.tjxt.tjcommon.Constants.ErrorInfo;
import com.tjxt.tjcommon.Model.Response.R;
import com.tjxt.tjcommon.Utils.ApiSignHelper;
import com.tjxt.tjcommon.Utils.JsonUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * 请求签名校验过滤器（业务接口"防篡改"的落地入口）。
 *
 * 适用场景：服务端↔服务端、合作方回调等**跨信任域**调用。浏览器前端不要用（私钥在前端等于没有密钥），
 * 前端请求的防篡改靠服务端权威校验 + JWT（见 docs/authentication.md）。
 * 内网全链路如果需要"传输层"级别的保障，优先用 mTLS（见 docs/internal-tls.md），不必叠加本过滤器。
 *
 * 校验顺序（顺序很重要）：
 *   1. 路径是否命中 include-path（没命中直接放行，零开销）
 *   2. 四个签名头是否齐全、格式是否合法
 *   3. 时间戳是否在容差内（双向）
 *   4. appId 是否已配置（拿服务端固定的公钥，不用请求里的）
 *   5. 签名是否正确（先验签，再消费 nonce —— 否则攻击者可以用无效签名刷掉你的 nonce）
 *   6. nonce 是否首次出现（Redis SETNX，防重放）
 * 任何一步失败 → 401 + R 格式 JSON（对外只给笼统信息，细节只写日志，避免给攻击者做探针）。
 *
 * 通过后：把 appId 放进请求属性（{@link ApiSignHelper#ATTR_APP_ID}），业务侧可以据此区分调用方 / 做数据隔离。
 */
@Slf4j
public class ApiSignatureFilter implements Filter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ApiSignProperties properties;
    private final NonceStore nonceStore;
    private final ResourceLoader resourceLoader;

    /** 公钥解析结果缓存（验签是热路径，避免每个请求都解析 PEM / Base64） */
    private final Map<String, String> publicKeyCache = new ConcurrentHashMap<>();

    public ApiSignatureFilter(ApiSignProperties properties, NonceStore nonceStore, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.nonceStore = nonceStore;
        this.resourceLoader = resourceLoader;
        if (properties.getIncludePath().isEmpty()) {
            log.warn("请求签名校验已开启，但 tj.auth.sign.include-path 为空：不会校验任何请求（请补上要保护的路径）");
        } else {
            log.info("请求签名校验已开启：保护路径={}，已配置调用方={} 个", properties.getIncludePath(),
                    properties.getApps().keySet());
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest request)
                || !(servletResponse instanceof HttpServletResponse response)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 1. 路径匹配：只保护配置的路径，其余请求完全不参与（性能与兼容性最好）
        String checkPath = resolveCheckPath(request);
        if (!shouldCheck(checkPath)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        String appId = trim(request.getHeader(AuthConstants.SIGN_APP_ID_HEADER));
        String signature = trim(request.getHeader(AuthConstants.SIGN_SIGNATURE_HEADER));
        String nonce = trim(request.getHeader(AuthConstants.SIGN_NONCE_HEADER));
        String timestampHeader = trim(request.getHeader(AuthConstants.SIGN_TIMESTAMP_HEADER));

        // 2. 头必须齐全且格式合法
        if (!ApiSignHelper.validAppId(appId) || !ApiSignHelper.validNonce(nonce)
                || !StringUtils.hasText(signature) || !StringUtils.hasText(timestampHeader)) {
            reject(request, response, "签名请求头缺失或格式非法", appId, checkPath);
            return;
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader);
        } catch (NumberFormatException e) {
            reject(request, response, "时间戳格式非法", appId, checkPath);
            return;
        }

        // 3. 时间戳容差（双向：过期与未来时间都拒绝）
        if (!ApiSignHelper.timestampFresh(timestamp, properties.getTolerance())) {
            reject(request, response, "请求时间戳超出容差（可能已过期或是重放）", appId, checkPath);
            return;
        }

        // 4. 取服务端固定的公钥（绝不用请求里的公钥）
        String publicKey = resolvePublicKey(appId);
        if (!StringUtils.hasText(publicKey)) {
            reject(request, response, "调用方未配置公钥（appId=" + appId + "）", appId, checkPath);
            return;
        }

        // 5. 读请求体（带大小上限）并验签
        byte[] body;
        try {
            body = readBody(request, properties.getMaxBodyBytes());
        } catch (IllegalArgumentException e) {
            reject(request, response, e.getMessage(), appId, checkPath);
            return;
        }
        String canonicalPathAndQuery = checkPath + (StringUtils.hasText(request.getQueryString())
                ? "?" + request.getQueryString() : "");
        if (!ApiSignHelper.verifyRequest(appId, request.getMethod(), canonicalPathAndQuery,
                timestamp, nonce, body, signature, publicKey)) {
            reject(request, response, "请求签名校验失败（数据可能被篡改，或双方待签串不一致）", appId, checkPath);
            return;
        }

        // 6. 防重放：验签通过后才消费 nonce
        if (!nonceStore.tryUse(appId, nonce, properties.getNonceTtl())) {
            reject(request, response, "nonce 重复（疑似重放请求）", appId, checkPath);
            return;
        }

        log.debug("请求签名校验通过：appId={}, method={}, path={}", appId, request.getMethod(), canonicalPathAndQuery);
        request.setAttribute(ApiSignHelper.ATTR_APP_ID, appId);
        // 请求体已读出，需要用包装后的 request 继续传递，否则后面的 Controller 读不到 body
        chain.doFilter(body.length == 0 ? request : new BodyCachedRequest(request, body), response);
    }

    // ==================== 内部实现 ====================

    /** 验签用的路径 = 去掉 context-path 与可选前缀后的路径（签名方必须签同一个字符串） */
    private String resolveCheckPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        String prefix = properties.getStripPathPrefix();
        if (StringUtils.hasText(prefix) && path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        return path.isEmpty() ? "/" : path;
    }

    /** 是否命中需要校验的路径（exclude 优先） */
    private boolean shouldCheck(String path) {
        for (String pattern : properties.getExcludePath()) {
            if (PATH_MATCHER.match(pattern, path)) {
                return false;
            }
        }
        for (String pattern : properties.getIncludePath()) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /** 公钥来源：配置里直接给 Base64，或给 PEM 文件路径；解析结果缓存 */
    private String resolvePublicKey(String appId) {
        String cached = publicKeyCache.get(appId);
        if (cached != null) {
            return cached;
        }
        ApiSignProperties.AppKey appKey = properties.getApps().get(appId);
        if (appKey == null) {
            return null;
        }
        try {
            String publicKey = StringUtils.hasText(appKey.getPublicKey())
                    ? normalizeBase64(appKey.getPublicKey())
                    : readPemBase64(appKey.getPublicKeyPath());
            if (StringUtils.hasText(publicKey)) {
                publicKeyCache.put(appId, publicKey);
            }
            return publicKey;
        } catch (Exception e) {
            log.error("解析调用方公钥失败：appId={}，来源={}", appId,
                    StringUtils.hasText(appKey.getPublicKey()) ? "配置 public-key" : appKey.getPublicKeyPath(), e);
            return null;
        }
    }

    private static String normalizeBase64(String base64) {
        return base64.replaceAll("\\s", "");
    }

    /** 读 PEM 文件（去掉头尾与空白后即 Base64(SPKI)，与 EcdsaHelper 期望的格式一致） */
    private String readPemBase64(String location) throws IOException {
        if (!StringUtils.hasText(location)) {
            return null;
        }
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("公钥文件不存在: " + location);
        }
        try (var in = resource.getInputStream()) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // 启动即校验能否解析成 EC 公钥，避免配错文件后到线上才发现
            String base64 = pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
                    .replaceAll("-----END [A-Z ]+-----", "")
                    .replaceAll("\\s", "");
            KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
            return base64;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("公钥不是合法的 EC(SPKI) 公钥: " + location, e);
        }
    }

    /** 读请求体（带大小上限），用于计算摘要 */
    private static byte[] readBody(HttpServletRequest request, int maxBytes) throws IOException {
        long declared = request.getContentLengthLong();
        if (declared > maxBytes) {
            throw new IllegalArgumentException("请求体过大（" + declared + " > " + maxBytes + "），拒绝做签名校验");
        }
        byte[] body = request.getInputStream().readAllBytes();
        if (body.length > maxBytes) {
            throw new IllegalArgumentException("请求体过大（" + body.length + " > " + maxBytes + "），拒绝做签名校验");
        }
        return body;
    }

    /** 拒绝请求：对外笼统信息 + 401，细节只进日志 */
    private void reject(HttpServletRequest request, HttpServletResponse response,
                        String reason, String appId, String path) throws IOException {
        log.warn("请求签名校验未通过：reason={}, appId={}, method={}, path={}, remote={}",
                reason, appId, request.getMethod(), path, request.getRemoteAddr());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JsonUtils.toJsonStr(
                R.error(ErrorInfo.Code.UNAUTHORIZED, "请求签名校验未通过")));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 请求体缓存包装：body 已经被过滤器读过一次，用它让下游（Controller）还能照常读。
     */
    private static final class BodyCachedRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private BodyCachedRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // 非异步读取，无需实现
                }

                @Override
                public int read() {
                    return in.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
