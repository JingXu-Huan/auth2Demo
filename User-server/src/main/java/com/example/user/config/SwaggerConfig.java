package com.example.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * Swagger 配置
 */
@Configuration
@EnableSwagger2WebMvc
public class SwaggerConfig {
    

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * @return Docket
     */
    @Bean
    Docket authApi() {
        return SwaggerCore.defaultDocketBuilder("认证管理",
                "com.example.User.controller", "OAuth2 授权服务器");
    }
}
