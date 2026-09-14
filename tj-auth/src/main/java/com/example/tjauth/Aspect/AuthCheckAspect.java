package com.example.tjauth.Aspect;


import com.example.tjauth.Utils.AuthUtils;
import com.example.tjcommon.Model.Dto.LoginUserDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/*
 * 动态路径权限校验切面。
 * 职责：拦截所有 Controller 方法，调用 AuthUtil 校验"路径 → 角色"。
 * 说明：仅 auth 服务使用（权限数据在 auth 库）。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuthCheckAspect {

    private final AuthUtils authUtils;

    @Before("execution(* com.example.tjauth.Controller..*.*(..))")
    public void checkAuth(JoinPoint pjp) {
        // 1. 拿到当前请求路径
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;
        HttpServletRequest request = attrs.getRequest();
        String path = request.getRequestURI();

        // 2. 拿到当前用户
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        LoginUserDTO user = principal instanceof LoginUserDTO u ? u : null;

        // 3. 动态权限校验（未配置权限的路径会放行）
        authUtils.checkAuth(path, user);
    }
}