package com.tjxt.tjcommon.Autoconfigure.Mvc;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import com.tjxt.tjcommon.Utils.DateUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Fastjson2 序列化配置。
 * 替代默认的 Jackson，统一日期格式、时区，并将 Long/BigInteger 序列化为字符串（避免前端精度丢失）。
 * <p>说明：本类是 Fastjson2 消息转换器的**唯一注册点**（MvcConfig 不再重复注册同名 Bean，
 * 否则 Spring Boot 会因 bean 定义覆盖（BeanDefinitionOverrideException）导致服务启动失败）；
 * 返回类型写成具体的 {@link FastJsonHttpMessageConverter}，便于其他配置按类型注入。
 */
@Configuration
@ConditionalOnClass(FastJsonHttpMessageConverter.class)
public class Fastjson2Config {

    @Bean
    @ConditionalOnMissingBean(FastJsonHttpMessageConverter.class)
    public FastJsonHttpMessageConverter fastJsonHttpMessageConverter() {
        // 设置 JVM 默认时区为东八区（Fastjson2 会使用该时区）
        TimeZone.setDefault(TimeZone.getTimeZone(DateUtils.TIME_ZONE_8));

        var config = new FastJsonConfig();
        // 日期格式 & 时区
        config.setDateFormat(DateUtils.DEFAULT_DATE_TIME_FORMAT);
        // 序列化特性：Long → String，以及浏览器兼容（解决 JS 精度问题）
        config.setWriterFeatures(
                JSONWriter.Feature.WriteLongAsString,
                JSONWriter.Feature.BrowserCompatible
        );
        // 字符编码（可选）
        var converter = new FastJsonHttpMessageConverter();
        converter.setFastJsonConfig(config);
        converter.setDefaultCharset(UTF_8);
        return converter;
    }

}
