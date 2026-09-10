package com.example.tj_project_apicommon.Autoconfigure.Mvc.Advice;

import com.example.tj_project_apicommon.Constants.Constant;
import com.example.tj_project_apicommon.Model.Response.R;
import com.example.tj_project_apicommon.Exceptions.CommonException;
import com.example.tj_project_apicommon.Utils.WebUtils;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

/*
 * 全局异常处理器
 * 职责：统一处理 Controller 层抛出的各类异常，返回标准 R 响应体（含 requestId）。
 */
@RestControllerAdvice
@Slf4j
public class CommonExceptionAdvice {

    // ---------- gRPC 异常 ----------
    @ExceptionHandler(StatusRuntimeException.class)
    public Object handleGrpcException(StatusRuntimeException e) {
        log.error("gRPC 调用异常 -> ", e);
        // 返回 500，并显示错误描述
        return processResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "gRPC 服务异常: " + e.getStatus().getDescription());
    }

    // ---------- 数据库异常 ----------
    @ExceptionHandler(DataAccessException.class)
    public Object handleDataAccessException(DataAccessException e) {
        log.error("数据库操作异常 -> ", e);
        return processResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "数据库操作失败，请稍后重试");
    }

    // ---------- 自定义业务异常 ----------
    @ExceptionHandler(CommonException.class)
    public Object handleCommonException(CommonException e) {
        log.error("自定义异常 -> {} , 状态码：{}, 异常原因：{}", e.getClass().getName(), e.getStatus(), e.getMessage());
        log.debug("", e);
        return processResponse(e.getStatus(), e.getCode(), e.getMessage());
    }

    // ---------- 参数校验异常 ----------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors()
                .stream().map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining("|"));
        log.error("请求参数校验异常 -> {}", msg);
        log.debug("", e);
        return processResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.value(), msg);
    }

    @ExceptionHandler(BindException.class)
    public Object handleBindException(BindException e) {
        log.error("请求参数绑定异常 -> {}", e.getMessage());
        log.debug("", e);
        return processResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.value(), "请求参数格式错误");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Object handleConstraintViolationException(ConstraintViolationException e) {
        log.error("请求参数约束异常 -> {}", e.getMessage());
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .collect(Collectors.joining("|"));
        return processResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.value(), msg);
    }

    // ---------- Servlet 异常 ----------
    @ExceptionHandler(ServletException.class)
    public Object handleServletException(ServletException e) {
        log.error("Servlet 异常 -> {}", e.getMessage());
        log.debug("", e);
        return processResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.value(), "请求处理异常");
    }

    // ---------- 其他未捕获异常（兜底） ----------
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, WebRequest request) {
        log.error("未处理的异常，URI: {} -> ", request.getDescription(false), e);
        return processResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "服务器内部异常");
    }

    // ---------- 统一响应封装 ----------
    private Object processResponse(int httpStatus, int bizCode, String msg) {
        // 标记响应已处理
         WebUtils.setResponseHeader(Constant.BODY_PROCESSED_MARK_HEADER, "true");

        // 统一返回 R 对象（含 requestId）
        return R.error(bizCode, msg).requestId(MDC.get(Constant.REQUEST_ID_HEADER));
    }
}
