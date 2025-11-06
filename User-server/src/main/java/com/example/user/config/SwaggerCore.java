package com.example.user.config;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/** 
 * @author Junjie
 * @version 1.0.0
 * @Date 2025-11-06
 * Swagger 核心配置工具类
 */
public class SwaggerCore {
    
    /**
     * 默认的 Docket 构建器
     * @author Junjie
     * @version 1.0.0
     * @Date 2025-11-06
     * @param groupName 分组名称
     * @param basePackage 扫描的包路径
     * @param title 文档标题
     * @return Docket
     * 
     * Docket是什么?
     * Docket是Swagger的配置类，用于配置Swagger的文档信息。
     * DocumentationType.SWAGGER_2：指定使用Swagger 2.0规范。
     * groupName：分组名称，用于区分不同的API文档。
     * apiInfo：API文档的基本信息，包括标题、描述、版本等。
     * select：选择要扫描的API。为空时，扫描所有API。
     * apis：指定要扫描的包路径。为空时，扫描所有包。
     * paths：指定要扫描的路径。为空时，扫描所有路径。
     * build：构建Docket对象。
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
     * @author Junjie
     * @version 1.0.0
     * @Date 2025-11-06
     * @param title 文档标题
     * @return ApiInfo
     * 
     * ApiInfo是什么?
     * ApiInfo是Swagger的配置类，用于配置Swagger的文档信息。
     * ApiInfoBuilder：用于构建ApiInfo对象。
     * title：文档标题。
     * description：文档描述。
     * version：文档版本。
     * contact：联系人信息。
     * license：许可证。
     * licenseUrl：许可证URL。
     * build：构建ApiInfo对象。
     */
    private static ApiInfo buildApiInfo(String title) {
        return new ApiInfoBuilder()
                .title(title + " API 文档")
                .description("User 服务的api文档")
                .version("1.0.0")
                .contact(new Contact("Junjie", "", ""))
                .license("Apache 2.0")
                .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0")
                .build();
    }
}
