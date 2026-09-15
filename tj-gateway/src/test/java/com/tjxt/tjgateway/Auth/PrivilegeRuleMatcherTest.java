package com.tjxt.tjgateway.Auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * 权限规则解析与匹配测试（纯逻辑，不依赖 Redis）。
 * 覆盖：Redis JSON 解析（method:uri / roles / internal）、方法匹配、ant 风格路径、路由前缀双候选、空规则放行。
 */
class PrivilegeRuleMatcherTest {

    @Test
    @DisplayName("解析 Redis 中的权限缓存：METHOD:/uri + 角色集合")
    void parseRule() {
        PrivilegeRule rule = PrivilegeRule.parse("GET:/users/{id}",
                "{\"id\":1,\"antPath\":\"GET:/users/{id}\",\"internal\":false,\"roles\":[1,2]}");

        assertNotNull(rule);
        assertEquals("GET:/users/{id}", rule.antPath());
        assertEquals(HttpMethod.GET, rule.method());
        assertEquals("/users/{id}", rule.uri());
        assertEquals(Set.of(1L, 2L), rule.roles());
        assertFalse(rule.internal());
    }

    @Test
    @DisplayName("解析兼容：roles 为字符串、antPath 缺失时用 Hash field 兜底、非法 JSON 返回 null")
    void parseEdgeCases() {
        PrivilegeRule stringRoles = PrivilegeRule.parse("POST:/courses",
                "{\"roles\":[\"1\",\"3\"],\"internal\":true}");
        assertNotNull(stringRoles);
        assertEquals(Set.of(1L, 3L), stringRoles.roles());
        assertTrue(stringRoles.internal());
        assertEquals("/courses", stringRoles.uri());

        assertNull(PrivilegeRule.parse("GET:/x", "这不是JSON"));
    }

    @Test
    @DisplayName("方法和 ant 路径都要匹配才命中")
    void matchMethodAndPath() {
        List<PrivilegeRule> rules = List.of(
                rule("POST:/courses", 1L),
                rule("GET:/users/**", 2L));

        assertNotNull(PrivilegeRuleMatcher.findRule(rules, HttpMethod.POST, List.of("/courses")));
        assertNotNull(PrivilegeRuleMatcher.findRule(rules, HttpMethod.GET, List.of("/users/1/profile")));
        // 方法不符
        assertNull(PrivilegeRuleMatcher.findRule(rules, HttpMethod.GET, List.of("/courses")));
        // 路径不符
        assertNull(PrivilegeRuleMatcher.findRule(rules, HttpMethod.DELETE, List.of("/orders/1")));
    }

    @Test
    @DisplayName("路由前缀双候选：/user/users/1 与 /users/1 都能命中后端登记的规则")
    void matchWithRoutePrefix() {
        List<PrivilegeRule> rules = List.of(rule("GET:/users/{id}", 1L));

        String gatewayPath = "/user/users/1";
        List<String> candidates = List.of(gatewayPath, PrivilegeRuleMatcher.stripFirstSegment(gatewayPath));

        assertEquals("/users/1", candidates.get(1));
        assertNotNull(PrivilegeRuleMatcher.findRule(rules, HttpMethod.GET, candidates));
        // 已经是后端路径（没有可去的路由前缀）时只会多出一个无意义的候选（/1），不会误命中 /users/**
        assertEquals("/1", PrivilegeRuleMatcher.stripFirstSegment("/users/1"));
        assertNull(PrivilegeRuleMatcher.findRule(rules, HttpMethod.GET, List.of("/1")));
    }

    @Test
    @DisplayName("未配置权限的路径：返回 null（调用方按“登录即可访问”处理）")
    void noRuleMeansNoRequirement() {
        assertNull(PrivilegeRuleMatcher.findRule(List.of(), HttpMethod.GET, List.of("/anything")));
        assertNull(PrivilegeRuleMatcher.findRule(null, HttpMethod.GET, List.of("/anything")));
        assertNull(PrivilegeRuleMatcher.findRule(List.of(rule("GET:/users/**", 1L)), HttpMethod.GET, List.of("/courses")));
    }

    @Test
    @DisplayName("角色判定：roles 为空表示不限制角色；否则必须包含当前 roleId")
    void allowsRole() {
        assertTrue(rule("GET:/a", 1L, 2L).allows(2L));
        assertFalse(rule("GET:/a", 1L, 2L).allows(3L));
        assertFalse(rule("GET:/a", 1L, 2L).allows(null));
        // 未配置角色：登录即可
        assertTrue(new PrivilegeRule("GET:/b", HttpMethod.GET, "/b", Set.of(), false).allows(null));
    }

    // ==================== 测试辅助 ====================

    private PrivilegeRule rule(String antPath, Long... roles) {
        return PrivilegeRule.parse("field:" + antPath,
                "{\"antPath\":\"%s\",\"roles\":%s}".formatted(antPath, java.util.Arrays.toString(roles)));
    }
}
