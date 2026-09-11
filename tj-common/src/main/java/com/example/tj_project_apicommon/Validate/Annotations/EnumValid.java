package com.example.tj_project_apicommon.Validate.Annotations;

import com.example.tj_project_apicommon.Validate.EnumValidator;
import com.example.tj_project_apicommon.Validate.EnumValueValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/*
 * 状态枚举校验注解。
 * 职责：用于校验字段/参数值是否在指定的枚举值范围内，配合 EnumValidator 和 EnumValueValidator 使用。
 * 使用：@EnumValid(enumeration = {1, 2, 3}, message = "用户类型错误")
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@Constraint(validatedBy = {EnumValidator.class, EnumValueValidator.class})
public @interface EnumValid {

    String message() default "不满足业务条件";

    int[] enumeration() default {};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}