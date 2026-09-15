package com.tjxt.tjgateway.Auth;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/*
 * 权限规则（一条"路径 → 允许的角色"）。
 * 数据来源：auth 服务写入 Redis Hash `auth:privileges` 的 PrivilegeRoleDTO（field = "METHOD:/uri"）。
 * 说明：
 *   - antPath 与老项目保持一致：由权限表的 method + uri 拼接（如 GET:/users/{id}）
 *   - roles 为角色ID集合，与登录用户 LoginUserDTO.roleId 比对（与老项目 AuthUtil#checkAuth 口径一致）
 *   - internal 表示"内部接口"，当前不参与网关校验（与 auth 服务 AuthUtils 行为保持一致）
 */
@Slf4j
public record PrivilegeRule(String antPath, HttpMethod method, String uri, Set<Long> roles, boolean internal) {

    /**
     * 解析 Redis 中的一条权限缓存。
     *
     * @param field  Hash field，即 antPath（METHOD:/uri）
     * @param json   Hash value，PrivilegeRoleDTO 的 JSON
     * @return 权限规则；数据非法时返回 null（跳过该条，不影响其它规则）
     */
    public static PrivilegeRule parse(String field, String json) {
        try {
            JSONObject object = JSON.parseObject(json);
            String antPath = object.getString("antPath");
            if (!StringUtils.hasText(antPath)) {
                antPath = field;
            }
            if (!StringUtils.hasText(antPath)) {
                return null;
            }
            // 1. 拆分 method 与 uri：形如 "GET:/users/{id}"；无前缀时视为不限方法
            HttpMethod method = null;
            String uri = antPath.trim();
            int index = uri.indexOf(':');
            if (index > 0) {
                String methodName = uri.substring(0, index).trim();
                String path = uri.substring(index + 1).trim();
                HttpMethod parsed = parseMethod(methodName);
                if (parsed != null && StringUtils.hasText(path)) {
                    method = parsed;
                    uri = path;
                }
            }
            // 2. 角色ID集合（兼容数字与字符串两种写法）
            Set<Long> roles = new LinkedHashSet<>();
            JSONArray roleArray = object.getJSONArray("roles");
            if (roleArray != null) {
                for (Object role : roleArray) {
                    Long roleId = toLong(role);
                    if (roleId != null) {
                        roles.add(roleId);
                    }
                }
            }
            boolean internal = Boolean.TRUE.equals(object.getBoolean("internal"));
            return new PrivilegeRule(antPath.trim(), method, uri, roles, internal);
        } catch (Exception e) {
            log.warn("权限缓存解析失败（field={}）：{}", field, e.getMessage());
            return null;
        }
    }

    /**
     * 角色是否允许访问该路径。
     * <p>规则未配置任何角色时视为不限制角色（只需要登录），与老项目行为一致。
     */
    public boolean allows(Long roleId) {
        if (roles.isEmpty()) {
            return true;
        }
        return roleId != null && roles.contains(roleId);
    }

    private static HttpMethod parseMethod(String name) {
        try {
            return HttpMethod.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
