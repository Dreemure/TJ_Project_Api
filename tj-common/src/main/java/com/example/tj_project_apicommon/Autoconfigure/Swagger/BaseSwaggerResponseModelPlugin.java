package com.example.tj_project_apicommon.Autoconfigure.Swagger;

import com.example.tj_project_apicommon.Model.Response.R;
import com.fasterxml.classmate.TypeResolver;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.core.Ordered;
import java.util.LinkedHashMap;

/*
 * Swagger 响应模型注册器（springdoc-openapi 版）。
 * 职责：将统一响应体 R 的模型定义注册到 OpenAPI components/schemas 中，
 * 使文档中 R 类型可被复用，与 BaseSwaggerResponseBuilderPlugin 协同工作。
 * 使用：注册为 Spring Bean，springdoc 自动加载。
 * 说明：springdoc 在解析响应体时会自动注册泛型实例化后的模型，本类兜底注册无泛型 R，
 * 避免某些只有 void 返回值的接口在文档中缺失 R 定义。
 */
public class BaseSwaggerResponseModelPlugin implements OpenApiCustomizer, Ordered {

    private final TypeResolver typeResolver = new TypeResolver();

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 12;
    }

    @Override
    public void customise(OpenAPI openApi) {
        // 1. 解析无泛型 R 的 schema
        ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .readAllAsResolvedSchema(typeResolver.resolve(R.class));

        // 2. 确保 components / schemas 已初始化
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        if (openApi.getComponents().getSchemas() == null) {
            openApi.getComponents().setSchemas(new LinkedHashMap<>());
        }

        // 3. 注册引用的 schema（直接访问字段，不是 getter）
        if (resolvedSchema.referencedSchemas != null) {
            openApi.getComponents().getSchemas().putAll(resolvedSchema.referencedSchemas);
        }

        // 4. 注册 R 本身（直接访问字段）
        if (resolvedSchema.schema != null) {
            String schemaName = resolvedSchema.schema.getName() != null
                    ? resolvedSchema.schema.getName()
                    : "R";
            openApi.getComponents().getSchemas().putIfAbsent(schemaName, resolvedSchema.schema);
        }
    }
}
