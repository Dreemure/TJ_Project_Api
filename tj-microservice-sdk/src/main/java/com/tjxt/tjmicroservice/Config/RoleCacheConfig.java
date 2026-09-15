package com.tjxt.tjmicroservice.Config;

import com.tjxt.tjmicroservice.AutoConfiguration.GrpcClientAutoConfiguration;
import com.tjxt.tjmicroservice.Cache.RoleCache;
import com.tjxt.tjmicroservice.Client.AuthGrpcClient;
import com.tjxt.tjmicroservice.Model.Dto.Auth.RoleDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/*
 * 角色缓存自动配置。
 * 职责：注册角色缓存（Caffeine）和角色缓存工具类（RoleCache）两个 Bean。
 * 说明：
 *   - RoleCache 依赖 AuthGrpcClient（认证服务 gRPC 客户端）；
 *     该客户端 Bean 由 GrpcClientAutoConfiguration 按"Stub 是否存在"决定是否注册，
 *     所以这里用 @ConditionalOnBean 守卫：没配置认证服务通道的服务不会因为缺 Bean 启动失败
 *   - 因此本类必须在 GrpcClientAutoConfiguration 之后加载（after 保证顺序，@ConditionalOnBean 才准）
 */
@AutoConfiguration(after = GrpcClientAutoConfiguration.class)
public class RoleCacheConfig {

    /**
     * 角色 Caffeine 缓存。
     * key = 角色id，value = 角色信息。
     */
    @Bean
    public Cache<Long, RoleDTO> roleCaches() {
        return Caffeine.newBuilder()
                .initialCapacity(1)                        // 初始容量
                .maximumSize(10_000)                       // 最大条目数
                .expireAfterWrite(Duration.ofMinutes(30))  // 写入后 30 分钟过期
                .build();
    }

    /**
     * 角色缓存工具类。
     *
     * @param roleCaches     角色缓存
     * @param authGrpcClient 认证服务 gRPC 客户端
     * @return RoleCache 实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AuthGrpcClient.class)
    public RoleCache roleCache(
            Cache<Long, RoleDTO> roleCaches,
            AuthGrpcClient authGrpcClient) {
        return new RoleCache(roleCaches, authGrpcClient);
    }
}
