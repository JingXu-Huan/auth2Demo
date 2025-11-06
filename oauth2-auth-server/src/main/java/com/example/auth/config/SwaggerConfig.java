package com.example.auth.config;

import com.example.common.config.SwaggerCore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * OAuth2 授权服务器 Swagger 配置
 * 
 * @author Cascade
 * @version 1.0.0
 */
@Configuration
@EnableSwagger2WebMvc
public class SwaggerConfig {
    
    @Bean
    Docket authApi() {
        return SwaggerCore.defaultDocketBuilder(
                "认证管理",
                "com.example.auth.controller", 
                "OAuth2 授权服务器");
    }
}
