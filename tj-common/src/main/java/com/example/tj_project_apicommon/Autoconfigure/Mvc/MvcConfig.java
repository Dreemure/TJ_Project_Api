package com.example.tj_project_apicommon.Autoconfigure.Mvc;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import com.example.tj_project_apicommon.Autoconfigure.Mvc.Advice.CommonExceptionAdvice;
import com.example.tj_project_apicommon.Autoconfigure.Mvc.Advice.WrapperResponseBodyAdvice;
import com.example.tj_project_apicommon.Autoconfigure.Mvc.Converter.WrapperResponseMessageConverter;
import com.example.tj_project_apicommon.Filters.RequestIdFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.TimeZone;
import jakarta.servlet.Filter;

/*
 * Spring MVC 公共配置
 * 职责：注册全局异常处理器、请求ID过滤器、响应包装转换器（基于Fastjson2）、响应体增强建议。 Fastjson2 作为消息转换器
 */
@ConditionalOnClass({CommonExceptionAdvice.class, Filter.class})
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Bean
    public CommonExceptionAdvice commonExceptionAdvice() {
        return new CommonExceptionAdvice();
    }

    @Bean
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    /**
     * Fastjson2 消息转换器
     */
    @Bean
    public FastJsonHttpMessageConverter fastJsonHttpMessageConverter() {
        // 设置 JVM 默认时区（东八区），Fastjson2 会使用该时区格式化日期
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));

        FastJsonConfig config = new FastJsonConfig();
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
        config.setWriterFeatures(
                JSONWriter.Feature.WriteLongAsString,   // Long → String，避免前端精度丢失
                JSONWriter.Feature.BrowserCompatible
        );

        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        converter.setFastJsonConfig(config);
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        return converter;
    }

    @Bean
    @ConditionalOnMissingClass("org.springframework.cloud.gateway.filter.GlobalFilter")
    public WrapperResponseMessageConverter wrapperResponseMessageConverter(
            FastJsonHttpMessageConverter fastJsonHttpMessageConverter   // 注入 Fastjson2 转换器
    ) {
        return new WrapperResponseMessageConverter(fastJsonHttpMessageConverter);
    }

    @Bean
    public WrapperResponseBodyAdvice wrapperResponseBodyAdvice() {
        return new WrapperResponseBodyAdvice();
    }
}
