package com.example.tj_project_apicommon.Autoconfigure.Mvc;

import com.example.tj_project_apicommon.Autoconfigure.Mvc.Aspects.CheckerAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * 参数校验切面配置类。
 * 职责：手动将 CheckerAspect 实例注册为 Spring Bean，使其生效。
 * 说明：因 CheckerAspect 类本身仅标注 @Aspect，未标注 @Component，故通过此配置类显式创建 Bean。
 */
@Configuration
public class ParamCheckerConfig {

    @Bean
    public CheckerAspect checkerAspect(){
        return new CheckerAspect();
    }
}
