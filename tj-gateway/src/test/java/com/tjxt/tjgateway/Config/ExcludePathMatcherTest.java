package com.tjxt.tjgateway.Config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * 网关免认证白名单匹配测试。
 * 覆盖：纯路径、方法:路径、ANY:路径、空白配置、方法不匹配等（与原项目 tj.auth.exclude-path 写法兼容）。
 */
class ExcludePathMatcherTest {

    @Test
    @DisplayName("纯路径写法：匹配任意方法")
    void plainPathMatchesAnyMethod() {
        ExcludePathMatcher matcher = new ExcludePathMatcher(Set.of("/error/**", "/jwks"));

        assertTrue(matcher.matches(HttpMethod.GET, "/error/500"));
        assertTrue(matcher.matches(HttpMethod.POST, "/jwks"));
        assertTrue(matcher.matches(HttpMethod.GET, "/jwks"));
        assertFalse(matcher.matches(HttpMethod.GET, "/accounts/login"));
    }

    @Test
    @DisplayName("方法:路径写法：只放行指定方法")
    void methodScopedPattern() {
        ExcludePathMatcher matcher = new ExcludePathMatcher(Set.of("POST:/accounts/login"));

        assertTrue(matcher.matches(HttpMethod.POST, "/accounts/login"));
        assertFalse(matcher.matches(HttpMethod.GET, "/accounts/login"));
        assertEquals(Set.of(HttpMethod.POST), matcher.methodPatterns().keySet());
        assertEquals(0, matcher.plainPatterns().length);
    }

    @Test
    @DisplayName("ANY:/路径 与 *:/路径 等价于纯路径")
    void anyMethodPrefix() {
        ExcludePathMatcher matcher = new ExcludePathMatcher(Set.of("ANY:/doc.html", "*:/webjars/**"));

        assertTrue(matcher.matches(HttpMethod.GET, "/doc.html"));
        assertTrue(matcher.matches(HttpMethod.DELETE, "/webjars/a.js"));
        assertEquals(2, matcher.plainPatterns().length);
        assertTrue(matcher.methodPatterns().isEmpty());
    }

    @Test
    @DisplayName("空白配置被忽略，未配置时视为空白名单")
    void blankEntriesIgnored() {
        ExcludePathMatcher matcher = new ExcludePathMatcher(Arrays.asList(" ", "/actuator/**", ""));

        assertEquals(1, matcher.size());
        assertFalse(matcher.isEmpty());
        assertTrue(matcher.matches(HttpMethod.GET, "/actuator/health"));
        assertTrue(new ExcludePathMatcher(List.of()).isEmpty());
        assertTrue(new ExcludePathMatcher(null).isEmpty());
    }

    @Test
    @DisplayName("方法为空（非标准请求）时不匹配方法限定规则，但仍匹配纯路径规则")
    void nullMethod() {
        ExcludePathMatcher matcher = new ExcludePathMatcher(Set.of("POST:/accounts/login", "/error/**"));

        assertFalse(matcher.matches(null, "/accounts/login"));
        assertTrue(matcher.matches(null, "/error/500"));
    }
}
