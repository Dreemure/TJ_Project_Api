package com.tjxt.tjcommon.Autoconfigure.Mvc;

import com.tjxt.tjcommon.Autoconfigure.Mvc.Aspects.CheckerAspect;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * 参数校验切面配置类。
 * 职责：手动将 CheckerAspect 实例注册为 Spring Bean，使其生效。
 * 说明：因 CheckerAspect 类本身仅标注 @Aspect，未标注 @Component，故通过此配置类显式创建 Bean。
 */
@Configuration
@ConditionalOnClass(Aspect.class)   // 未引入 aspectj 的模块自动跳过
public class ParamCheckerConfig {

    @Bean
    public CheckerAspect checkerAspect(){
        return new CheckerAspect();
    }
}
