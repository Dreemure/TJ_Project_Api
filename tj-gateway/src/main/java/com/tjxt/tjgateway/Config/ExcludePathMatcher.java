package com.tjxt.tjgateway.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/*
 * 免认证白名单匹配器。
 * 职责：把 tj.auth.exclude-path 解析成规则，供 SecurityWebFilterChain（授权规则）与
 *       JwtAuthenticationFilter（白名单放行）共用，保证“哪条路径免认证”只有一处口径。
 * 支持的写法（与原项目 tj.auth.exclude-path 兼容）：
 *   1. 纯路径（ant 风格，匹配任意方法）：/error/**、/jwks、/doc.html
 *   2. 方法:路径：POST:/accounts/login、GET:/accounts/refresh
 *   3. *:路径 / ANY:路径：等价于纯路径
 * 注意：路径匹配使用 Spring 的 AntPathMatcher（与 SecurityWebFilterChain 的 PathPattern 语义基本一致）。
 */
@Slf4j
public class ExcludePathMatcher {

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    /** 全部规则（方法为 null 表示不限制方法） */
    private final List<Rule> rules;
    /** 不限制方法的路径模式，用于 SecurityWebFilterChain 的 pathMatchers(...) */
    private final String[] plainPatterns;
    /** 按方法分组的路径模式，用于 SecurityWebFilterChain 的 pathMatchers(method, ...) */
    private final Map<HttpMethod, List<String>> methodPatterns;

    public ExcludePathMatcher(Collection<String> configs) {
        this.rules = parse(configs);

        List<String> plain = new ArrayList<>();
        Map<HttpMethod, List<String>> byMethod = new LinkedHashMap<>();
        for (Rule rule : rules) {
            if (rule.method() == null) {
                plain.add(rule.pattern());
            } else {
                byMethod.computeIfAbsent(rule.method(), key -> new ArrayList<>()).add(rule.pattern());
            }
        }
        this.plainPatterns = plain.toArray(String[]::new);
        this.methodPatterns = byMethod;
    }

    /**
     * 判断请求是否命中白名单。
     *
     * @param method 请求方法，可为 null
     * @param path   请求路径
     * @return true 表示免认证
     */
    public boolean matches(HttpMethod method, String path) {
        if (path == null) {
            return false;
        }
        for (Rule rule : rules) {
            if (rule.method() != null && !rule.method().equals(method)) {
                continue;
            }
            if (ANT_PATH_MATCHER.match(rule.pattern(), path)) {
                return true;
            }
        }
        return false;
    }

    /** 是否没有任何白名单配置。 */
    public boolean isEmpty() {
        return rules.isEmpty();
    }

    /** 不限制方法的路径模式。 */
    public String[] plainPatterns() {
        return plainPatterns.clone();
    }

    /** 按方法分组的路径模式。 */
    public Map<HttpMethod, List<String>> methodPatterns() {
        return methodPatterns;
    }

    /** 全部规则数量，便于启动日志排查。 */
    public int size() {
        return rules.size();
    }

    // ==================== 解析 ====================

    private List<Rule> parse(Collection<String> configs) {
        List<Rule> result = new ArrayList<>();
        if (configs == null) {
            return result;
        }
        for (String config : configs) {
            if (!StringUtils.hasText(config)) {
                continue;
            }
            String value = config.trim();
            int index = value.indexOf(':');
            if (index > 0) {
                String methodName = value.substring(0, index).trim();
                String pattern = value.substring(index + 1).trim();
                if (StringUtils.hasText(pattern)) {
                    if ("*".equals(methodName) || "ANY".equalsIgnoreCase(methodName)) {
                        result.add(new Rule(null, pattern));
                        continue;
                    }
                    HttpMethod method = resolveMethod(methodName);
                    if (method != null) {
                        result.add(new Rule(method, pattern));
                        continue;
                    }
                }
            }
            // 纯路径写法：不限制请求方法
            result.add(new Rule(null, value));
        }
        if (!result.isEmpty()) {
            log.debug("免认证白名单解析结果：{}", result);
        }
        return result;
    }

    private HttpMethod resolveMethod(String name) {
        try {
            return HttpMethod.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            // 不是合法的方法名，按纯路径处理
            return null;
        }
    }

    /**
     * 白名单规则。
     *
     * @param method  请求方法；null 表示不限制
     * @param pattern ant 风格路径模式
     */
    private record Rule(HttpMethod method, String pattern) {
    }
}
