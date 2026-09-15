package com.tjxt.tjgateway.Auth;

import com.alibaba.fastjson2.JSON;
import com.tjxt.tjcommon.Constants.AuthConstants;
import com.tjxt.tjgateway.Config.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/*
 * 权限缓存（网关侧）。
 * 职责：从 Redis 读取 auth 服务写入的"路径 → 角色"权限表，缓存到本地供鉴权使用。
 * 数据协议（与 auth 服务 PrivilegeServiceImpl 一致）：
 *   - Hash  auth:privileges ：field = "METHOD:/uri"，value = PrivilegeRoleDTO 的 JSON
 *   - String version        ：权限版本号，变化说明权限数据被修改
 * 说明：
 *   - 定时任务（默认 20 秒，tj.auth.privilege.refresh-interval）先比对版本号，版本未变则不重复读数据
 *   - 使用响应式 Redis 客户端，不阻塞事件循环；首次拉取由定时任务的第一次执行完成（不阻塞网关启动）
 *   - 降级策略：Redis 不可用时保留上一次快照并打告警；快照为空时"未配置权限"，只做认证不做角色校验
 *     （与原项目 AuthUtil 在权限缓存为空时的行为一致）
 */
@Slf4j
@Component
public class PrivilegeCache {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final AuthProperties authProperties;

    /** 权限规则快照（整体替换，避免读到半成品） */
    private volatile List<PrivilegeRule> rules = List.of();
    /** 本地缓存的版本号（初始 -1，保证首次一定刷新） */
    private volatile int version = -1;
    /** 最近一次成功加载时间（0 表示尚未成功加载过） */
    private volatile long lastLoadedAt;
    /** 最近一次错误信息，用于避免重复刷屏 */
    private volatile String lastError;

    public PrivilegeCache(ReactiveStringRedisTemplate redisTemplate, AuthProperties authProperties) {
        this.redisTemplate = redisTemplate;
        this.authProperties = authProperties;
    }

    /**
     * 定时刷新权限缓存。
     * <p>首次执行即完成初始化加载，因此不需要在启动阶段阻塞等待 Redis。
     */
    @Scheduled(fixedDelayString = "${tj.auth.privilege.refresh-interval:20s}")
    public void refreshTask() {
        if (!authProperties.getPrivilege().isEnabled()) {
            return;
        }
        try {
            Duration timeout = authProperties.getPrivilege().getTimeout();
            // 1. 读取版本号：未变化则跳过（减少 Redis 读取）
            String rawVersion = redisTemplate.opsForValue()
                    .get(AuthConstants.AUTH_PRIVILEGE_VERSION_KEY)
                    .block(timeout);
            int currentVersion = parseVersion(rawVersion);
            if (lastLoadedAt > 0 && currentVersion == version) {
                return;
            }
            // 2. 读取权限表
            Map<String, String> entries = redisTemplate.<String, String>opsForHash()
                    .entries(AuthConstants.AUTH_PRIVILEGE_KEY)
                    .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                    .block(timeout);
            List<PrivilegeRule> parsed = parse(entries);
            // 3. 原子替换
            this.rules = parsed;
            this.version = currentVersion;
            this.lastLoadedAt = System.currentTimeMillis();
            this.lastError = null;
            log.info("网关权限缓存已刷新：版本 {}，共 {} 条规则", currentVersion, parsed.size());
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            if (!message.equals(lastError)) {
                this.lastError = message;
                log.warn("读取权限缓存失败（将保留上一次快照，仅认证不鉴权）：{}", message);
            }
        }
    }

    /**
     * 查找匹配的权限规则。
     *
     * @param method 请求方法
     * @param paths  路径候选（原始路径 + 去掉路由前缀后的路径）
     * @return 命中的规则；null 表示该路径未配置权限
     */
    public PrivilegeRule findRule(HttpMethod method, List<String> paths) {
        return PrivilegeRuleMatcher.findRule(rules, method, paths);
    }

    /** 当前规则数量。 */
    public int ruleCount() {
        return rules.size();
    }

    /** 是否已经成功加载过（用于启动日志判断是否降级）。 */
    public boolean isLoaded() {
        return lastLoadedAt > 0;
    }

    private List<PrivilegeRule> parse(Map<String, String> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<PrivilegeRule> parsed = new ArrayList<>(entries.size());
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            PrivilegeRule rule = PrivilegeRule.parse(entry.getKey(), entry.getValue());
            if (rule != null) {
                parsed.add(rule);
            }
        }
        return Collections.unmodifiableList(parsed);
    }

    private int parseVersion(String rawVersion) {
        if (rawVersion == null || rawVersion.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(rawVersion.trim());
        } catch (NumberFormatException e) {
            log.debug("权限版本号格式非法：{}", rawVersion);
            return 0;
        }
    }
}
