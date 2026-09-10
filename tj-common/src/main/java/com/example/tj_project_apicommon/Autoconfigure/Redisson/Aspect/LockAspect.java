package com.example.tj_project_apicommon.Autoconfigure.Redisson.Aspect;

import com.example.tj_project_apicommon.Autoconfigure.Redisson.Annotations.Lock;
import com.example.tj_project_apicommon.Exceptions.BizIllegalException;
import com.example.tj_project_apicommon.Utils.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

@Aspect
@Component
public class LockAspect {

    private final RedissonClient redissonClient;

    public LockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 环绕通知：加锁 → 执行方法 → 释放锁。
     */
    @Around("@annotation(properties)")
    public Object handleLock(ProceedingJoinPoint pjp, Lock properties) throws Throwable {
        // 1. 校验：不自动释放锁时，必须指定 leaseTime
        if (!properties.autoUnlock() && properties.leaseTime() <= 0) {
            throw new BizIllegalException("leaseTime不能为空");
        }
        // 2. 解析锁名（支持 SpEL）
        String name = getLockName(properties.name(), pjp);
        // 3. 获取锁对象
        RLock rLock = properties.lockType().getLock(redissonClient, name);
        // 4. 尝试加锁
        boolean success = properties.lockStrategy().tryLock(rLock, (java.util.concurrent.locks.Lock) properties);
        if (!success) {
            return null;
        }
        try {
            // 5. 执行目标方法
            return pjp.proceed();
        } finally {
            // 6. 释放锁
            if (properties.autoUnlock()) {
                rLock.unlock();
            }
        }
    }

    // ==================== SpEL 锁名解析 ====================

    /** SpEL 占位符正则：匹配 #{...} */
    private static final Pattern SPEL_PATTERN = Pattern.compile("#\\{([^}]*)}");

    /** 方法参数名解析器 */
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    /** SpEL 解析器（线程安全，可复用） */
    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    /**
     * 解析锁名，支持 #{expression} 形式的 SpEL 表达式。
     *
     * @param name 原始锁名模板，如 "order:#{orderId}"
     * @param pjp  切入点
     * @return 解析后的锁名
     */
    private String getLockName(String name, ProceedingJoinPoint pjp) {
        if (StringUtils.isBlank(name) || !name.contains("#{")) {
            return name;
        }
        // 1. 构建 SpEL 上下文
        EvaluationContext context = new MethodBasedEvaluationContext(
                TypedValue.NULL, resolveMethod(pjp), pjp.getArgs(), PARAMETER_NAME_DISCOVERER);

        // 2. 循环替换所有占位符
        var matcher = SPEL_PATTERN.matcher(name);
        var result = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            result.append(name, lastEnd, matcher.start());
            String expr = matcher.group(1).trim();
            // 3. 解析表达式（#param 形式引用参数）
            Expression expression = EXPRESSION_PARSER.parseExpression(
                    expr.startsWith("T(") ? expr : "#" + expr);
            Object value = expression.getValue(context);
            result.append(value == null ? "" : value);
            lastEnd = matcher.end();
        }
        result.append(name, lastEnd, name.length());
        return result.toString();
    }

    /**
     * 从切点解析目标方法（优先取目标类的方法，兼容 CGLIB 代理）。
     */
    private Method resolveMethod(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Class<?> clazz = pjp.getTarget().getClass();
        String name = signature.getName();
        Class<?>[] parameterTypes = signature.getMethod().getParameterTypes();
        return tryGetDeclaredMethod(clazz, name, parameterTypes);
    }

    /**
     * 递归查找方法（含父类）。
     */
    private Method tryGetDeclaredMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        if (clazz == null) {
            return null;
        }
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return tryGetDeclaredMethod(clazz.getSuperclass(), name, parameterTypes);
        }
    }
}