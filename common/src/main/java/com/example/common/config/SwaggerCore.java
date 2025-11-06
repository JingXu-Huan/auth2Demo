package com.example.common.config;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/** 
 * Swagger 核心配置工具类
 * 提供统一的 Swagger 文档配置
 * 
 * @author Common Module
 * @version 1.0.0
 */
public class SwaggerCore {
    
    /**
     * 默认的 Docket 构建器
     * 
     * @param groupName 分组名称，用于区分不同的API文档
     * @param basePackage 扫描的包路径
     * @param title 文档标题
     * @return Docket 配置对象
     */
    public static Docket defaultDocketBuilder(String groupName, String basePackage, String title) {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName(groupName)
                .apiInfo(buildApiInfo(title, "API 文档"))
                .select()
                .apis(RequestHandlerSelectors.basePackage(basePackage))
                .paths(PathSelectors.any())
                .build();
    }
    
    /**
     * 自定义 Docket 构建器
     * 
     * @param groupName 分组名称
     * @param basePackage 扫描的包路径
     * @param title 文档标题
     * @param description 文档描述
     * @return Docket 配置对象
     */
    public static Docket customDocketBuilder(String groupName, String basePackage, String title, String description) {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName(groupName)
                .apiInfo(buildApiInfo(title, description))
                .select()
                .apis(RequestHandlerSelectors.basePackage(basePackage))
                .paths(PathSelectors.any())
                .build();
    }
    
    /**
     * 构建 API 基本信息
     * 
     * @param title 文档标题
     * @param description 文档描述
     * @return ApiInfo 对象
     */
    private static ApiInfo buildApiInfo(String title, String description) {
        return new ApiInfoBuilder()
                .title(title + " API 文档")
                .description(description)
                .version("1.0.0")
                .contact(new Contact("OAuth2 Demo Team", "", ""))
                .license("Apache 2.0")
                .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0")
                .build();
    }
}
