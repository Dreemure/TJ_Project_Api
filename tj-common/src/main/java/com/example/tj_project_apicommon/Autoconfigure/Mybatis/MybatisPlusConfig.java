package com.example.tj_project_apicommon.Autoconfigure.Mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import com.example.tj_project_apicommon.Utils.TableNameContext;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Random;

/*
 * MyBatis-Plus 插件注入器
 */
@Configuration
@ConditionalOnClass({MybatisPlusInterceptor.class, BaseMapper.class}) // 条件判断：类路径中是否存在指定类。若不存在，整个配置类失效，不加载任何 Bean
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 动态表名
        DynamicTableNameInnerInterceptor dynamicInterceptor = new DynamicTableNameInnerInterceptor(
                (sql, tableName) -> {
                    String year = TableNameContext.getYear();
                    if (year == null) {
                        year = (new Random().nextInt(10) % 2 == 0) ? "_2018" : "_2019";
                    }
                    return tableName + "_" + year;
                }
        );
        interceptor.addInnerInterceptor(dynamicInterceptor);

        // 2. 数据变更记录（保留，弃用警告可忽略；若需移除则注释掉）
        DataChangeRecorderInnerInterceptor dataInterceptor = new DataChangeRecorderInnerInterceptor();
        dataInterceptor.setBatchUpdateLimit(1000).openBatchUpdateLimitation();
        interceptor.addInnerInterceptor(dataInterceptor);

        // 3. 分页
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        // 4. 乐观锁
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 5. 非法SQL拦截
        interceptor.addInnerInterceptor(new IllegalSQLInnerInterceptor());

        return interceptor;
    }
}