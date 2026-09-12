package com.example.tj_project_apimicroservice.Config;

import com.example.tj_project_apimicroservice.Cache.RoleCache;
import com.example.tj_project_apimicroservice.Client.AuthGrpcClient;
import com.example.tj_project_apimicroservice.Model.Dto.Auth.RoleDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/*
 * 角色缓存配置。
 * 职责：注册角色缓存（Caffeine）和角色缓存工具类（RoleCache）两个 Bean。
 */
@Configuration
public class RoleCacheConfig {

    /**
     * 角色 Caffeine 缓存。
     * key = 角色id，value = 角色信息。
     */
    @Bean
    public Cache<Long, RoleDTO> roleCaches() {
        return Caffeine.newBuilder()
                .initialCapacity(1)                       // 初始容量
                .maximumSize(10_000)                      // 最大条目数
                .expireAfterWrite(Duration.ofMinutes(30)) // 写入后 30 分钟过期
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
    public RoleCache roleCache(
            Cache<Long, RoleDTO> roleCaches,
            AuthGrpcClient authGrpcClient) {
        return new RoleCache(roleCaches, authGrpcClient);
    }
}
