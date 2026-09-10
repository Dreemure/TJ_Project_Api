package com.example.tj_project_apicommon.Autoconfigure.Swagger;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.io.Serializable;

/*
 * Swagger 配置属性类。
 * 职责：绑定 application.yml 中 tj.swagger.* 前缀的配置项，控制 Swagger 文档开关、响应包装及文档元信息（标题、描述、联系人、版本等）。
 * 使用：由 @EnableConfigurationProperties 注册，通过 @ConfigurationProperties 自动注入到 Knife4jConfiguration。
 * 注意：enable 和 enableResponseWrap 默认 false；其余字段无默认值，需在配置中显式指定。
 */
@Data
@ConfigurationProperties(prefix = "tj.swagger")
public class SwaggerConfigProperties implements Serializable {

    private Boolean enable = false;
    private Boolean enableResponseWrap = false;

    public String packagePath;

    public String title;

    public String description;

    public String contactName;

    public String contactUrl;

    public String contactEmail;

    public String version;
}
