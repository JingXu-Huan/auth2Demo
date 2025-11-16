package org.example.imserver;

import com.example.common.config.ServiceAuthConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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
