package com.example.tj_project_apicommon.Validate.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * 参数校验标记注解。
 * 职责：标记在方法上，配合 CheckerAspect 切面，自动触发参数校验逻辑。
 * 行为：当方法被调用时，CheckerAspect 会拦截并检查参数是否实现 Checker 接口或为 List 类型，执行对应的校验方法。
 * 使用：在需要自动校验的 Service/Controller 方法上标注 @ParamChecker，无需手动调用 check()。
 * 注意：需配合 CheckerAspect 和 Checker 接口使用；无额外属性，纯标记型注解。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ParamChecker {
}