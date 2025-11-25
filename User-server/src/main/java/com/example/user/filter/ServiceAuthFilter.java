package com.example.user.filter;

import com.example.common.config.ServiceAuthConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 服务间认证过滤器
 * 保护内部接口，只允许持有有效 Token 的服务访问
 */
@Component
@Order(1)
@Slf4j
public class ServiceAuthFilter extends OncePerRequestFilter {
    
    @Autowired
    private ServiceAuthConfig authConfig;
    
    // 需要保护的内部接口路径（包含敏感信息）
    private static final List<String> INTERNAL_API_PATTERNS = Arrays.asList(
        "/api/users/details/",      // 包含敏感信息的用户详情接口
        "/api/users/internal/"      // 其他内部接口
    );
    
    // 公开接口，不需要服务认证
    private static final List<String> PUBLIC_API_PATTERNS = Arrays.asList(
        "/api/users/register",
        "/api/v1/users/register",
        "/api/v1/users/confirm",
        "/api/v1/users/exists/",
        "/api/v1/users/check-email",
        "/api/v1/users/check-email",
        "/api/v1/users/check-username",
        "/api/v1/users/check-username",
        "/api/v1/users/details/email/",
        "/api/v1/users/update-login-time",
        "/api/v1/email/",
        "/api/v1/security/",
        "/actuator/",
        "/swagger",
        "/v2/api-docs",
        "/doc.html"
    );

    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 过滤器核心方法
     * 检查请求URI并验证服务认证Token
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        log.info("ServiceAuthFilter 处理请求: URI={}", requestURI);
        
        // 检查是否是公开接口
        boolean isPublicApi = PUBLIC_API_PATTERNS.stream()
            .anyMatch(requestURI::contains);
        
        log.info("是否公开接口: {}, URI={}", isPublicApi, requestURI);
        
        if (isPublicApi) {
            // 公开接口，直接放行
            log.info("公开接口放行: URI={}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        
        // 检查是否是内部接口
        boolean isInternalApi = INTERNAL_API_PATTERNS.stream()
            .anyMatch(requestURI::contains);
        
        if (isInternalApi) {
            // 内部接口，需要验证服务 Token
            String serviceToken = request.getHeader("X-Service-Auth");
            
            if (serviceToken == null || serviceToken.trim().isEmpty()) {
                log.warn("缺少服务认证Token: URI={}, IP={}", 
                        requestURI, request.getRemoteAddr());
                
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"访问被拒绝：缺少服务认证信息\"}");
                return;
            }
            
            if (!authConfig.validateServiceToken(serviceToken)) {
                log.warn("无效的服务认证Token: URI={}, IP={}", 
                        requestURI, request.getRemoteAddr());
                
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"访问被拒绝：无效的服务认证\"}");
                return;
            }
            
            log.debug("服务认证通过: URI={}", requestURI);
        }
        
        filterChain.doFilter(request, response);
    }
}
