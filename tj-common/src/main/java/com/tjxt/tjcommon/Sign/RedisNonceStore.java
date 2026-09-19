package com.tjxt.tjcommon.Sign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/*
 * 基于 Redis 的 nonce 去重（多实例部署的正确做法）。
 * 实现：SET key value NX EX ttl —— 原子地"不存在才写入"，天然适合一次性标记。
 * key 设计：auth:sign:nonce:<appId>:<nonce>，带 TTL 自动清理，不会无限增长。
 */
@Slf4j
public class RedisNonceStore implements NonceStore {

    /** nonce 记录的 Redis key 前缀 */
    public static final String NONCE_KEY_PREFIX = "auth:sign:nonce:";

    private final StringRedisTemplate redisTemplate;

    public RedisNonceStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryUse(String appId, String nonce, Duration ttl) {
        String key = NONCE_KEY_PREFIX + appId + ":" + nonce;
        try {
            Boolean first = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
            return Boolean.TRUE.equals(first);
        } catch (Exception e) {
            // Redis 不可用时 fail-closed：无法判断是否重放，就不放行
            log.error("nonce 去重失败（Redis 异常），按疑似重放拒绝：appId={}, key={}", appId, key, e);
            return false;
        }
    }
}
