package org.example.imserver.filter;

import com.example.common.config.ServiceAuthConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-13
 * IM消息服务间认证过滤器
 * 保护内部接口，只允许持有有效 Token 的服务访问
 */
@Component
@Order(2)  // 在 GatewayOnlyFilter 之后执行
@Slf4j
public class ServiceAuthFilter extends OncePerRequestFilter {
    
    @Autowired
    private ServiceAuthConfig authConfig;
    
    // 需要保护的内部接口路径（包含敏感信息）
    private static final List<String> INTERNAL_API_PATTERNS = Arrays.asList(
        "/internal/",                   // 所有内部接口
        "/api/chat/internal/",         // 内部聊天接口
        "/api/chat/admin/",            // 管理接口
        "/api/chat/system/"            // 系统接口
    );
    
    // 公开接口，不需要服务认证
    private static final List<String> PUBLIC_API_PATTERNS = Arrays.asList(
        "/api/chat/send",              // 公开消息发送接口
        "/api/chat/health",            // 健康检查
        "/api/chat/online-users",      // 在线用户数
        "/api/chat/private/",          // 私聊接口
        "/api/chat/group/",            // 群聊接口
        "/ws/",                        // WebSocket连接
        "/actuator/",                  // 监控端点
        "/swagger",                    // API文档
        "/v2/api-docs",               // Swagger文档
        "/doc.html",                   // 文档页面
        "/index.html",                 // 测试页面
        "/static/",                    // 静态资源
        "/favicon.ico"                 // 图标
    );
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-13
     * 过滤器核心方法
     * 检查请求URI并验证服务认证Token
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        
        // 检查是否是公开接口
        boolean isPublicApi = PUBLIC_API_PATTERNS.stream()
            .anyMatch(requestURI::contains);
        
        if (isPublicApi) {
            // 公开接口，直接放行
            filterChain.doFilter(request, response);
            return;
        }
        
        // 检查是否是内部接口
        boolean isInternalApi = INTERNAL_API_PATTERNS.stream()
            .anyMatch(requestURI::contains);
        
        if (isInternalApi) {
            // 内部接口，需要验证服务 Token
            String token = request.getHeader("X-Service-Auth");
            if (token == null || token.trim().isEmpty()) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token == null || token.trim().isEmpty()) {
                log.warn("缺少或无效的服务认证Token: URI={}, IP={}", 
                        requestURI, request.getRemoteAddr());
                
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"访问被拒绝：缺少服务认证信息\"}");
                return;
            }
            if (!authConfig.validateServiceToken(token)) {
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
