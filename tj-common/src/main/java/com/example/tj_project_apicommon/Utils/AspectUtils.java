package com.example.tj_project_apicommon.Utils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import java.lang.reflect.Method;

/*
 * AOP 切面通用工具类
 */
public class AspectUtils {
    private AspectUtils(){}

    /*
     * 获取被拦截方法对象
     * MethodSignature.getMethod() 获取的是顶层接口或者父类的方法对象
     * 所以应该使用反射获取当前对象的方法对象
     */
    public static Method getMethod(ProceedingJoinPoint pjp) throws NoSuchMethodException {
        var signature = pjp.getSignature();
        if (!(signature instanceof MethodSignature methodSignature)) {
            throw new IllegalArgumentException("It's not method");
        }
        // 1. 先获取接口方法
        var method = methodSignature.getMethod();
        // 2. 如果代理的是实现类（CGLIB），则从目标类获取实际方法
        var targetClass = pjp.getTarget().getClass();
        if (!method.getDeclaringClass().isAssignableFrom(targetClass)) {
            // 查找实现类中对应的桥接/合成方法
            method = targetClass.getMethod(method.getName(), method.getParameterTypes());
        }
        return method;
    }

    /**
     * 在aop切面中SPEL表达式对formatter进行格式化，
     * 转换出指定的值
     */
    public static String parse(String formatter, Method method, Object[] args) {
        StandardReflectionParameterNameDiscoverer nameDiscoverer = new StandardReflectionParameterNameDiscoverer();
        return SPELUtils.parse(formatter, nameDiscoverer.getParameterNames(method), args);
    }
}
