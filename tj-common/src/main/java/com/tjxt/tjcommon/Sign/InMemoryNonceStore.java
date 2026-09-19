package com.tjxt.tjcommon.Sign;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * 单机内存版 nonce 去重（仅用于本地调试 / 单元测试）。
 * 注意：多实例部署时**不具备完整的防重放能力**（重放请求落到另一个实例就检测不到），
 * 因此 ApiSignatureFilter 只有在确实拿不到 Redis 时才回退到它，并打 WARN。
 */
@Slf4j
public class InMemoryNonceStore implements NonceStore {

    /** appId:nonce -> 过期时间（毫秒） */
    private final Map<String, Long> used = new ConcurrentHashMap<>();
    private volatile long lastCleanAt = System.currentTimeMillis();

    @Override
    public boolean tryUse(String appId, String nonce, Duration ttl) {
        long now = System.currentTimeMillis();
        cleanExpired(now);
        long expireAt = now + ttl.toMillis();
        // putIfAbsent 保证并发下同一个 nonce 只有一个请求能拿到 null（即首次使用）
        Long previous = used.putIfAbsent(appId + ":" + nonce, expireAt);
        if (previous == null) {
            return true;
        }
        if (previous < now) {
            // 上一次记录已过期，视为新的（正常不应发生：ttl 覆盖了时间戳容差）
            return used.replace(appId + ":" + nonce, previous, expireAt);
        }
        return false;
    }

    /** 低频清理，避免内存无限增长（最多每分钟一次） */
    private void cleanExpired(long now) {
        if (now - lastCleanAt < 60_000L) {
            return;
        }
        lastCleanAt = now;
        int before = used.size();
        used.entrySet().removeIf(entry -> entry.getValue() < now);
        log.debug("清理过期的 nonce 记录：{} -> {}", before, used.size());
    }
}
