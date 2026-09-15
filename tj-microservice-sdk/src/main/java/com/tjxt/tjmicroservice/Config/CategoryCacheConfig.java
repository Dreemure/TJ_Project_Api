package com.tjxt.tjmicroservice.Config;

import com.tjxt.tjmicroservice.AutoConfiguration.GrpcClientAutoConfiguration;
import com.tjxt.tjmicroservice.Cache.CategoryCache;
import com.tjxt.tjmicroservice.Client.CourseGrpcClient;
import com.tjxt.tjmicroservice.Model.Dto.Course.CategoryBasicDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.Map;

/*
 * 课程分类缓存自动配置。
 * 职责：注册分类缓存（Caffeine）与分类缓存工具类（CategoryCache）两个 Bean。
 * 说明：
 *   - CategoryCache 依赖 CourseGrpcClient（课程服务 gRPC 客户端）；
 *     该客户端 Bean 由 GrpcClientAutoConfiguration 按"Stub 是否存在"决定是否注册，
 *     所以这里用 @ConditionalOnBean 守卫：没配置课程服务通道的服务不会因为缺 Bean 启动失败
 *   - 因此本类必须在 GrpcClientAutoConfiguration 之后加载（after 保证顺序，@ConditionalOnBean 才准）
 */
@AutoConfiguration(after = GrpcClientAutoConfiguration.class)
public class CategoryCacheConfig {

    /** 分类缓存：key = 固定标识（CATEGORY），value = 分类id → 分类信息 */
    @Bean
    public Cache<String, Map<Long, CategoryBasicDTO>> categoryCaches() {
        return Caffeine.newBuilder()
                .initialCapacity(1)                        // 初始容量
                .maximumSize(10_000)                       // 最大条目数
                .expireAfterWrite(Duration.ofMinutes(30))  // 写入后 30 分钟过期
                .build();
    }

    /** 分类缓存工具类 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(CourseGrpcClient.class)
    public CategoryCache categoryCache(
            Cache<String, Map<Long, CategoryBasicDTO>> categoryCaches,
            CourseGrpcClient courseGrpcClient) {
        return new CategoryCache(categoryCaches, courseGrpcClient);
    }
}
