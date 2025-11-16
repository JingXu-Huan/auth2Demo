package org.example.imserver.util;

import com.example.common.config.ServiceAuthConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-13
 * 服务间调用工具类
 * 用于调用其他微服务的内部API
 */
@Component
@Slf4j
public class ServiceCallUtil {

    private final ServiceAuthConfig authConfig;
    private final RestTemplate restTemplate;

    @Autowired
    public ServiceCallUtil(ServiceAuthConfig authConfig) {
        this.authConfig = authConfig;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 调用其他服务的内部API
     * @param serviceUrl 目标服务URL
     * @param endpoint API端点
     * @param method HTTP方法
     * @param requestBody 请求体
     * @return 响应结果
     */
    public ResponseEntity<Map> callInternalApi(String serviceUrl, String endpoint, 
                                              HttpMethod method, Object requestBody) {
        try {
            // 生成服务间认证Token
            String token = authConfig.generateServiceToken();
            if (token == null) {
                log.error("Failed to generate service token");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            // 构建请求实体
            HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);

            // 构建完整URL
            String fullUrl = serviceUrl.endsWith("/") ? serviceUrl + endpoint : serviceUrl + "/" + endpoint;

            log.debug("Calling internal API: {} {}", method, fullUrl);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.exchange(
                fullUrl, method, requestEntity, Map.class
            );

            log.debug("Internal API call successful: {} - {}", fullUrl, response.getStatusCode());
            return response;

        } catch (Exception e) {
            log.error("Internal API call failed: {} {}", method, endpoint, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET请求的便捷方法
     */
    public ResponseEntity<Map> get(String serviceUrl, String endpoint) {
        return callInternalApi(serviceUrl, endpoint, HttpMethod.GET, null);
    }

    /**
     * POST请求的便捷方法
     */
    public ResponseEntity<Map> post(String serviceUrl, String endpoint, Object requestBody) {
        return callInternalApi(serviceUrl, endpoint, HttpMethod.POST, requestBody);
    }

    /**
     * PUT请求的便捷方法
     */
    public ResponseEntity<Map> put(String serviceUrl, String endpoint, Object requestBody) {
        return callInternalApi(serviceUrl, endpoint, HttpMethod.PUT, requestBody);
    }

    /**
     * DELETE请求的便捷方法
     */
    public ResponseEntity<Map> delete(String serviceUrl, String endpoint) {
        return callInternalApi(serviceUrl, endpoint, HttpMethod.DELETE, null);
    }
}
