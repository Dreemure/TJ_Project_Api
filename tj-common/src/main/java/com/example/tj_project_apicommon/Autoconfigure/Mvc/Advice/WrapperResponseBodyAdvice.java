package com.example.tj_project_apicommon.Autoconfigure.Mvc.Advice;

import com.example.tj_project_apicommon.Constants.Constant;
import com.example.tj_project_apicommon.Model.Response.R;
import com.example.tj_project_apicommon.Utils.WebUtils;
import io.reactivex.rxjava3.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;


/*
 * 响应体统一包装增强器（仅网关请求生效）。
 * 职责：将 Controller 返回的非 R 类型响应体，自动包装为 R<T> 统一格式，并注入链路追踪ID。
 * 行为：若返回类型已是 R 或 /v2/api-docs 路径，则直接放行；否则包装为 R.ok(body)，并设置 requestId。
 * 使用：通过 @RestControllerAdvice 全局生效，仅在 WebUtils.isGatewayRequest() 为 true 时触发。
 */
@RestControllerAdvice
/*
@RestControllerAdvice 是 Spring MVC 提供的全局增强注解，它是 @ControllerAdvice 和 @ResponseBody 的组合
它的核心作用是：

统一异常处理：配合 @ExceptionHandler，为所有 Controller 提供全局异常拦截（如你的 CommonExceptionAdvice）。

统一数据格式：配合 ResponseBodyAdvice，对 Controller 的返回值进行统一包装（如本类）。

数据绑定预处理：配合 @InitBinder 或 @ModelAttribute，对请求参数或模型进行全局定制。

生效范围：默认作用于所有标注了 @RestController 的类，也可通过 basePackages、assignableTypes 等属性限定范围。
 */
public class WrapperResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, @NonNull @org.jspecify.annotations.NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getParameterType() != R.class && WebUtils.isGatewayRequest();
    }

    @Override
    public Object beforeBodyWrite(
            Object body, @NonNull @org.jspecify.annotations.NonNull MethodParameter returnType, @NonNull @org.jspecify.annotations.NonNull MediaType selectedContentType,
            @NonNull @org.jspecify.annotations.NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request, @NonNull @org.jspecify.annotations.NonNull ServerHttpResponse response) {
        if (request.getURI().getPath().equals("/v2/api-docs")){
            return body;
        }
        if (body == null) {
            return R.ok().requestId(MDC.get(Constant.REQUEST_ID_HEADER));
        }
        if(body instanceof R){
            return body;
        }
        return R.ok(body).requestId(MDC.get(Constant.REQUEST_ID_HEADER));
    }
}
