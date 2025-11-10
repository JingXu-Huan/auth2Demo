package com.example.common.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * Springfox 与 Spring Boot 2.6+ 兼容性配置
 * 通过 BeanPostProcessor 修复 WebMvcRequestHandlerProvider 的空指针问题
 */
@Configuration

public class SpringfoxWebMvcConfiguration {

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * BeanPostProcessor
     * 修复 WebMvcRequestHandlerProvider 的空指针问题
     */
    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {

            /**
             * @author Junjie
             * @version 1.0.0
             * @date 2025-11-06
             * BeanPostProcessor
             * 修复 WebMvcRequestHandlerProvider 的空指针问题
             */
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
                }
                return bean;
            }

            /**
             * @author Junjie
             * @version 1.0.0
             * @date 2025-11-06
             * BeanPostProcessor
             * 修复 WebMvcRequestHandlerProvider 的空指针问题
             */
            private <T extends RequestMappingInfoHandlerMapping> void customizeSpringfoxHandlerMappings(List<T> mappings) {
                List<T> copy = mappings.stream()
                        .filter(mapping -> mapping.getPatternParser() == null)
                        .collect(Collectors.toList());
                mappings.clear();
                mappings.addAll(copy);
            }

            /**
             * @author Junjie
             * @version 1.0.0
             * @date 2025-11-06
             * BeanPostProcessor
             * 修复 WebMvcRequestHandlerProvider 的空指针问题
             */
            @SuppressWarnings("unchecked")
            private List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
                try {
                    Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
                    field.setAccessible(true);
                    return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }
}
