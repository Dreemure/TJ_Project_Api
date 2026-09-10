package com.example.tj_project_apicommon.Autoconfigure.Swagger;

import cn.hutool.core.convert.ConverterRegistry;
import com.example.tj_project_apicommon.Utils.TjTemporalConverter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import jakarta.annotation.Resource;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import java.time.LocalDateTime;

/*
 * Knife4j / springdoc-openapi 配置类。
 * 职责：配置 OpenAPI 文档信息（标题、描述、联系方式、版本）、按包路径分组扫描 Controller，
 * 并根据开关注册响应包装插件（R<T>）及 Hutool 日期转换器。
 * 使用：通过 tj.swagger.* 配置项控制；文档访问路径为 /doc.html。
 */
@Configuration
@ConditionalOnProperty(prefix = "tj.swagger", name = "enable", havingValue = "true")
@EnableConfigurationProperties(SwaggerConfigProperties.class)
public class Knife4jConfiguration {
    @Resource
    private SwaggerConfigProperties swaggerConfigProperties;

    /**
     * OpenAPI 全局信息（标题、描述、联系人、版本）。
     * springdoc 会将其应用为文档根信息。
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(swaggerConfigProperties.getTitle())
                        .description(swaggerConfigProperties.getDescription())
                        .version(swaggerConfigProperties.getVersion())
                        .contact(new Contact()
                                .name(swaggerConfigProperties.getContactName())
                                .url(swaggerConfigProperties.getContactUrl())
                                .email(swaggerConfigProperties.getContactEmail()))
                );
    }

    /**
     * 按包路径分组扫描 Controller。
     * 对应 Springfox 中 Docket 的 RequestHandlerSelectors.basePackage 配置。
     */
    @Bean
    public GroupedOpenApi defaultApi2() {
        return GroupedOpenApi.builder()
                .group("default")
                .packagesToScan(swaggerConfigProperties.getPackagePath())
                .build();
    }

    /**
     * 响应模型注册插件（仅当开启响应包装时生效）。
     * 将 R 模型兜底注册到 components/schemas。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "tj.swagger", name = "enableResponseWrap", havingValue = "true")
    public BaseSwaggerResponseModelPlugin baseSwaggerResponseModelPlugin() {
        return new BaseSwaggerResponseModelPlugin();
    }

    /**
     * 响应构建插件（仅当开启响应包装时生效）。
     * 将 Controller 返回值统一展示为 R<T>。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "tj.swagger", name = "enableResponseWrap", havingValue = "true")
    public BaseSwaggerResponseBuilderPlugin baseSwaggerResponseBuilderPlugin() {
        return new BaseSwaggerResponseBuilderPlugin();
    }

    /**
     * 初始化 Hutool 日期转换器（静态代码块，类加载时执行一次）。
     */
    static {
        ConverterRegistry converterRegistry = ConverterRegistry.getInstance();
        converterRegistry.putCustom(LocalDateTime.class, new TjTemporalConverter(LocalDateTime.class));
    }
}
