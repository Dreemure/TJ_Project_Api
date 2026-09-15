package com.tjxt.tjcommon.Autoconfigure.Mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import com.tjxt.tjcommon.Utils.TableNameContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/*
 * MyBatis-Plus 公共配置。
 * 职责：
 *   1. 注册 MybatisPlusInterceptor：动态表名（可选）、数据变更记录、分页、乐观锁、非法 SQL 拦截
 *   2. 注册审计字段自动填充（MetaObjectHandler）：create_time/creater/update_time/updater
 * 说明：
 *   - 动态表名默认**关闭**，由 tj.mybatis.dynamic-table.enabled=true 显式开启；开启后用
 *     TableNameContext 中的年份，取不到时用配置的 default-suffix（不再随机，避免写错表）
 *   - 需要自定义分表策略的服务，可直接声明自己的 DynamicTableNameInnerInterceptor Bean，
 *     并通过 @ConditionalOnMissingBean 的 mybatisPlusInterceptor 覆盖整体配置
 *   - 老项目由各模块提供 DynamicTableNameInnerInterceptor Bean，并在 MybatisConfig 中组装字段填充
 */
@Slf4j
@Configuration
@ConditionalOnClass({MybatisPlusInterceptor.class, BaseMapper.class})
@EnableConfigurationProperties(MybatisPlusConfig.DynamicTableProperties.class)
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 插件链。
     */
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(DynamicTableProperties tableProperties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 动态表名（默认关闭；开启后按 TableNameContext/default-suffix 拼后缀）
        if (tableProperties.isEnabled()) {
            interceptor.addInnerInterceptor(new DynamicTableNameInnerInterceptor((sql, tableName) -> {
                String suffix = TableNameContext.getYear();
                if (!StringUtils.hasText(suffix)) {
                    suffix = tableProperties.getDefaultSuffix();
                }
                if (!StringUtils.hasText(suffix)) {
                    return tableName;
                }
                String normalized = suffix.startsWith("_") ? suffix : "_" + suffix;
                return tableName + normalized;
            }));
            log.info("已开启 MyBatis-Plus 动态表名，默认后缀：{}", tableProperties.getDefaultSuffix());
        }

        // 2. 数据变更记录（批量更新上限保护）
        DataChangeRecorderInnerInterceptor dataInterceptor = new DataChangeRecorderInnerInterceptor();
        dataInterceptor.setBatchUpdateLimit(1000).openBatchUpdateLimitation();
        interceptor.addInnerInterceptor(dataInterceptor);

        // 3. 分页
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        // 4. 乐观锁
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 5. 非法 SQL 拦截
        interceptor.addInnerInterceptor(new IllegalSQLInnerInterceptor());

        return interceptor;
    }

    /**
     * 审计字段自动填充（创建人/更新人、创建时间/更新时间）。
     * <p>业务服务可实现自己的 MetaObjectHandler Bean 覆盖本实现。
     */
    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MetaObjectHandler metaObjectHandler() {
        return new MyBatisAutoFillHandler();
    }

    /*
     * 动态表名配置。
     */
    @Getter
    @Setter
    @ConfigurationProperties(prefix = "tj.mybatis.dynamic-table")
    public static class DynamicTableProperties {

        /** 是否开启动态表名（默认关闭，避免误拼表后缀） */
        private boolean enabled = false;

        /** TableNameContext 中没有年份时使用的默认后缀，如 _2024；为空表示不加后缀 */
        private String defaultSuffix;
    }
}
