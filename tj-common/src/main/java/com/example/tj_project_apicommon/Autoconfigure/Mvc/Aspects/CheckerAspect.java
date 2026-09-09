package com.example.tj_project_apicommon.Autoconfigure.Mvc.Aspects;

import com.example.tj_project_apicommon.Utils.ArrayUtils;
import com.example.tj_project_apicommon.Utils.CollUtils;
import com.example.tj_project_apicommon.Validate.Annotations.ParamChecker;
import com.example.tj_project_apicommon.Validate.Checker;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.util.List;

/*
 * 参数校验切面。
 * 职责：配合 @ParamChecker 注解，在方法执行前自动校验参数。
 * 行为：若参数实现 Checker 接口则调用 check()；若参数为 List 则调用 CollUtils.check() 进行批量校验。
 * 使用：在目标方法上标注 @ParamChecker，无需手动调用校验逻辑。
 */
@Aspect
@Slf4j
@SuppressWarnings("all")
public class CheckerAspect {

    @Before("@annotation(paramChecker)")
    public void before(JoinPoint joinPoint, ParamChecker paramChecker) {
        Object[] args = joinPoint.getArgs();
        if(ArrayUtils.isNotEmpty(args)){
            //遍历方法参数，参数是否实现了Checker接口
            for (Object arg : args){
                if(arg instanceof Checker) {
                    //调用check方法，校验业务逻辑
                    ((Checker)arg).check();
                }else if(arg instanceof List){
                    //如果参数是一个集合也要校验
                    CollUtils.check((List) arg);
                }
            }
        }
    }
}
