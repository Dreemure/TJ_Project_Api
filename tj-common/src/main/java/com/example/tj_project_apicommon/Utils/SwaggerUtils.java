package com.example.tj_project_apicommon.Utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;

/*
 * Swagger / OpenAPI 文档工具类（springdoc 版）。
 * 职责：提供 OpenAPI 文档的序列化与浅拷贝能力，便于自定义文档端点或做 URL 改写。
 * 使用：OpenAPI 对象由 Spring 容器注入，工具类只做无状态处理。
 * 注意：Springfox 的 SwaggerTransformationContext 在 springdoc 中不存在，已由本类替代。
 */
@Slf4j
public final class SwaggerUtils {

    private SwaggerUtils() {}

    /**
     * 将 OpenAPI 对象序列化为 JSON 字符串。
     *
     * @param openAPI OpenAPI 对象（由 Spring 容器注入）
     * @return JSON 字符串；序列化失败返回 null
     */
    public static String toJson(OpenAPI openAPI) {
        if (openAPI == null) {
            log.warn("OpenAPI 对象为空，无法序列化");
            return null;
        }
        try {
            return Json.mapper().writeValueAsString(openAPI);
        } catch (JsonProcessingException e) {
            log.error("序列化 OpenAPI 文档失败", e);
            return null;
        }
    }

    /**
     * 将 OpenAPI 对象序列化为 YAML 字符串。
     *
     * @param openAPI OpenAPI 对象（由 Spring 容器注入）
     * @return YAML 字符串；序列化失败返回 null
     */
    public static String toYaml(OpenAPI openAPI) {
        if (openAPI == null) {
            log.warn("OpenAPI 对象为空，无法序列化");
            return null;
        }
        try {
            return Json.mapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(openAPI);
        } catch (JsonProcessingException e) {
            log.error("序列化 OpenAPI 文档为 YAML 失败", e);
            return null;
        }
    }
}