package org.example.imgateway;

import com.example.common.config.ServiceAuthConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-13
 * IM-Gateway 应用主类
 * 启动 Spring Boot 应用
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,      // 排除数据源自动配置
    HibernateJpaAutoConfiguration.class     // 排除JPA自动配置
})
public class ImGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImGatewayApplication.class, args);
    }

    /**
     * 手动创建 ServiceAuthConfig Bean
     */
    @Bean
    public ServiceAuthConfig serviceAuthConfig() {
        return new ServiceAuthConfig();
    }

}
