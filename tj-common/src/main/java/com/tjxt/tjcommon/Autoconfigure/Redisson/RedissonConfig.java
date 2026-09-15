package com.tjxt.tjcommon.Autoconfigure.Redisson;

import com.tjxt.tjcommon.Autoconfigure.Redisson.Aspect.LockAspect;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

/*
 * Redisson 自动配置。
 * 职责：
 *   1. 用 spring.data.redis.* 的配置构造 RedissonClient（分布式锁、限流等 Redisson 能力的入口）
 *   2. 注册 @Lock 注解的切面 {@link LockAspect}（仅在 RedissonClient 存在时）
 * 说明：
 *   - 仅当显式配置了 spring.data.redis.host 时才装配：避免不需要 Redis 的服务（或未接 Redis 配置的服务）
 *     启动时去连 localhost 失败。Nacos 的 shared-redis.yaml 已提供该配置
 *   - 默认单机模式；如需哨兵/集群，自行定义 RedissonClient Bean 即可覆盖本配置
 *   - 老项目通过 spring.factories 注册 com.tianji.common.autoconfigure.redisson.RedissonConfig 提供等价能力
 *   - 两个 MetaObjectHandler/MQ 等其它公共装配见 org.springframework.boot.autoconfigure.AutoConfiguration.imports
 */
@Slf4j
@Configuration
@ConditionalOnClass({RedissonClient.class, Config.class})
@ConditionalOnProperty(prefix = "spring.data.redis", name = "host")
@EnableConfigurationProperties(RedissonConfig.RedisConnectionProperties.class)
public class RedissonConfig {

    /**
     * RedissonClient（单机模式）。
     *
     * @param props Redis 连接配置（spring.data.redis.*）
     * @return RedissonClient
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(RedisConnectionProperties props) {
        Config config = new Config();
        String address = "redis://%s:%d".formatted(props.getHost(), props.getPort());
        var single = config.useSingleServer()
                .setAddress(address)
                .setDatabase(props.getDatabase())
                .setTimeout((int) props.getTimeout().toMillis())
                .setConnectionMinimumIdleSize(props.getPoolMinIdle())
                .setConnectionPoolSize(props.getPoolMaxIdle());
        if (StringUtils.hasText(props.getPassword())) {
            single.setPassword(props.getPassword());
        }
        if (StringUtils.hasText(props.getClientName())) {
            single.setClientName(props.getClientName());
        }
        log.info("Redisson 初始化完成：address={}, database={}", address, props.getDatabase());
        return Redisson.create(config);
    }

    /**
     * 分布式锁切面：拦截 @Lock 注解方法。
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(LockAspect.class)
    public LockAspect lockAspect(RedissonClient redissonClient) {
        log.info("启用分布式锁切面 LockAspect（@Lock）");
        return new LockAspect(redissonClient);
    }

    /*
     * Redis 连接配置（与 Boot 的 spring.data.redis.* 同前缀，只取 Redisson 需要的字段）。
     * 说明：只读不写，不会覆盖 Boot 自己的 Redis 配置绑定。
     */
    @Getter
    @Setter
    @ConfigurationProperties(prefix = "spring.data.redis")
    public static class RedisConnectionProperties {

        /** Redis 主机 */
        private String host = "localhost";
        /** Redis 端口 */
        private int port = 6379;
        /** 密码，可空 */
        private String password;
        /** 数据库索引 */
        private int database = 0;
        /** 连接超时 */
        private Duration timeout = Duration.ofSeconds(3);
        /** 客户端名称（便于在 Redis 侧排查连接来源） */
        private String clientName;
        /** 连接池最小空闲连接（对齐 lettuce.pool.min-idle） */
        private int poolMinIdle = 1;
        /** 连接池最大连接（对齐 lettuce.pool.max-idle） */
        private int poolMaxIdle = 8;
    }
}
