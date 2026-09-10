package com.example.tj_project_apicommon.Autoconfigure.Swagger;

import com.example.tj_project_apicommon.Model.Response.R;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;

/*
 * Swagger 响应包装定制器
 * 职责：自动将 Controller 返回值包装为统一的 R<T>（void 返回则为 R<Void>），
 * 使 Swagger/Knife4j 文档展示的响应结构始终为 R 格式，与 WrapperResponseBodyAdvice 保持一致。
 * 使用：注册为 Spring Bean（@Bean 或 @Component），springdoc 自动加载生效。
 * 注意：若 Controller 返回值本身已是 R 类型，则跳过处理，避免二次包装。
 */
public class BaseSwaggerResponseBuilderPlugin implements OperationCustomizer {

    /**
     * 用于解析泛型类型（如 R<T>）
     */
    private final TypeResolver typeResolver = new TypeResolver();

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        // 1. 获取方法的原始返回类型
        Class<?> returnType = handlerMethod.getMethod().getReturnType();

        // 2. 已经是 R 类型，无需再包装
        if (R.class.isAssignableFrom(returnType)) {
            return operation;
        }

        // 3. 构造 R<T> 的 ResolvedType（void 则退化为 R<Void>）
        ResolvedType rType = (returnType == void.class || returnType == Void.class)
                ? typeResolver.resolve(R.class)
                : typeResolver.resolve(R.class, returnType);

        // 4. 解析 R<T> 的 Schema
        ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .readAllAsResolvedSchema(rType);

        // 5. 构造 200 响应体
        MediaType mediaType = new MediaType().schema(resolvedSchema.schema);
        Content content = new Content()
                .addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, mediaType);
        ApiResponse apiResponse = new ApiResponse()
                .description(message(handlerMethod))
                .content(content);

        // 6. 覆盖 200 响应（若原响应存在则替换）
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        responses.put(String.valueOf(HttpStatus.OK.value()), apiResponse);

        return operation;
    }

    /**
     * 从 @ResponseStatus 注解提取描述，若无则使用 HTTP 200 的默认短语。
     */
    private String message(HandlerMethod handlerMethod) {
        ResponseStatus responseStatus = AnnotatedElementUtils
                .findMergedAnnotation(handlerMethod.getMethod(), ResponseStatus.class);
        if (responseStatus != null) {
            String reason = responseStatus.reason();
            return reason.isEmpty() ? responseStatus.value().getReasonPhrase() : reason;
        }
        return HttpStatus.OK.getReasonPhrase();
    }
}
