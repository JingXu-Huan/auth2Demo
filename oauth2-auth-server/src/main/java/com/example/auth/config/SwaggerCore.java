package com.example.auth.config;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger 核心配置工具类
 * 
 * @author Cascade
 * @version 1.0.0
 */
public class SwaggerCore {
    
    /**
     * 默认的 Docket 构建器
     * 
     * @param groupName 分组名称
     * @param basePackage 扫描的包路径
     * @param title 文档标题
     * @return Docket
     */
    public static Docket defaultDocketBuilder(String groupName, String basePackage, String title) {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName(groupName)
                .apiInfo(buildApiInfo(title))
                .select()
                .apis(RequestHandlerSelectors.basePackage(basePackage))
                .paths(PathSelectors.any())
                .build();
    }
    
    /**
     * 构建 API 基本信息
     * 
     * @param title 文档标题
     * @return ApiInfo
     */
    private static ApiInfo buildApiInfo(String title) {
        return new ApiInfoBuilder()
                .title(title + " API 文档")
                .description("OAuth2 授权服务器的 RESTful API 接口文档")
                .version("1.0.0")
                .contact(new Contact("Junjie", "", ""))
                .license("Apache 2.0")
                .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0")
                .build();
    }
}
