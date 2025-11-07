package com.example.user.config;

import com.example.common.config.SwaggerCore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * User 服务 Swagger 配置
 */
@Configuration
@EnableSwagger2WebMvc
public class SwaggerConfig {
    
    @Bean
    Docket userApi() {
        return SwaggerCore.defaultDocketBuilder(
                "用户管理",
                "com.example.user.controller", 
                "User 服务");
    }
}
