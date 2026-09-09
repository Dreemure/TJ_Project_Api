package com.example.tj_project_apicommon.Autoconfigure.Mvc.Converter;

import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import com.example.tj_project_apicommon.Utils.WebUtils;
import jakarta.annotation.Nonnull;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.util.List;

/*
 * 响应包装消息转换器
 * 职责：仅在网关请求下，将响应体通过 Fastjson2 序列化并写入输出流（不处理读操作）。
 * 使用：作为 Spring MVC 的 HttpMessageConverter，由 MvcConfig 注入 FastJsonHttpMessageConverter 实例。
 * 注意：通过 canWrite 判断是否为网关请求，仅当为真时才执行序列化。
 */
public class WrapperResponseMessageConverter implements HttpMessageConverter<Object> {
    private final FastJsonHttpMessageConverter delegate;

    public WrapperResponseMessageConverter(FastJsonHttpMessageConverter fastJsonHttpMessageConverter) {
        this.delegate = fastJsonHttpMessageConverter;
    }

    @Override
    public boolean canRead(@Nonnull Class<?> clazz, MediaType mediaType) {
        return false;  // 不支持读操作
    }

    @Override
    public boolean canWrite(@Nonnull Class<?> clazz, MediaType mediaType) {
        return WebUtils.isGatewayRequest() && delegate.canWrite(clazz, mediaType);
    }

    @Override
    @Nonnull
    public List<MediaType> getSupportedMediaTypes() {
        return delegate.getSupportedMediaTypes();
    }

    @Override
    @Nonnull
    public Object read(@Nonnull Class<?> clazz, @Nonnull HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return delegate.read(clazz, inputMessage);
    }

    @Override
    public void write(@Nonnull Object o, MediaType contentType, @Nonnull HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        delegate.write(o, contentType, outputMessage);
    }
}
