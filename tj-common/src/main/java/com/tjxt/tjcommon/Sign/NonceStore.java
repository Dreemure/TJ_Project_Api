package com.tjxt.tjcommon.Sign;

import java.time.Duration;

/*
 * nonce 去重存储（防重放的核心）。
 * 职责：判断某个 (appId, nonce) 是否**首次**出现。
 * 安全要求：多实例部署必须用共享存储（见 RedisNonceStore），
 * 用单机内存实现时，重放请求只要落到另一个实例就检测不到。
 */
public interface NonceStore {

    /**
     * 尝试占用一个 nonce。
     *
     * @param appId 调用方标识
     * @param nonce 一次性随机串
     * @param ttl   记录保留时长（应覆盖时间戳容差，保证同一 nonce 在窗口内不会被放行两次）
     * @return true 表示首次使用（放行）；false 表示已经用过（疑似重放，拒绝）
     */
    boolean tryUse(String appId, String nonce, Duration ttl);
}
