package com.tjxt.tjgateway.Auth;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/*
 * 权限规则匹配器（纯函数，便于单测）。
 * 职责：在权限规则集合中找出与"请求方法 + 请求路径"匹配的规则。
 * 匹配口径：
 *   1. 方法：规则带方法时必须与请求方法一致；规则不带方法时不限方法
 *   2. 路径：ant 风格匹配（如 /users/**、/users/{id}）
 *   3. 路径候选：同时用"网关原始路径"与"去掉第一段前缀后的路径"去匹配，
 *      因为本项目路由使用 StripPrefix=1（/user/users/1 → /users/1），
 *      权限表里既可能登记网关路径，也可能登记后端接口路径，两种都能命中
 *   4. 多条命中时，取 patterns 中最先命中的一条（与老项目 AuthUtil#findMatchPath 行为一致）
 */
public final class PrivilegeRuleMatcher {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private PrivilegeRuleMatcher() {
        // 工具类私有构造，防止实例化
    }

    /**
     * 查找匹配的权限规则。
     *
     * @param rules  权限规则集合，可为空
     * @param method 请求方法，可为 null
     * @param paths  路径候选（按优先级排列，通常是 [原始路径, 去前缀路径]）
     * @return 命中的规则；没有配置任何相关规则时返回 null（表示"未配置权限"，放行）
     */
    public static PrivilegeRule findRule(List<PrivilegeRule> rules, HttpMethod method, List<String> paths) {
        if (rules == null || rules.isEmpty() || paths == null || paths.isEmpty()) {
            return null;
        }
        for (String path : paths) {
            if (path == null) {
                continue;
            }
            for (PrivilegeRule rule : rules) {
                if (rule.method() != null && !rule.method().equals(method)) {
                    continue;
                }
                if (PATH_MATCHER.match(rule.uri(), path)) {
                    return rule;
                }
            }
        }
        return null;
    }

    /**
     * 去掉路径的第一段（用于 StripPrefix=1 的反向匹配）。
     * <p>例：/user/users/1 → /users/1；/users/1 → /1（不会命中 /users/**，无副作用）。
     *
     * @param path 请求路径
     * @return 去掉首段后的路径；无法去段时返回原路径
     */
    public static String stripFirstSegment(String path) {
        if (path == null || path.isEmpty() || path.charAt(0) != '/') {
            return path;
        }
        int index = path.indexOf('/', 1);
        return index > 0 ? path.substring(index) : path;
    }
}
