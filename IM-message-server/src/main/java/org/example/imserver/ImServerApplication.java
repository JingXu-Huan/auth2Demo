package org.example.imserver;

import com.example.common.config.ServiceAuthConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * IM-message-server 服务
 * 功能: 提供即时聊天消息的 REST 接口与消息转发能力
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-16
 */
@SpringBootApplication
public class ImServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImServerApplication.class, args);
    }

    /**
     * 手动创建 ServiceAuthConfig Bean
     */
    @Bean
    public ServiceAuthConfig serviceAuthConfig() {
        return new ServiceAuthConfig();
    }

}
