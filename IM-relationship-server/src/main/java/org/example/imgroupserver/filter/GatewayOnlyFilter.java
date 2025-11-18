package org.example.imgroupserver.filter;

import lombok.extern.slf4j.Slf4j;
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

@Component
@Order(0)
@Slf4j
public class GatewayOnlyFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWED_DIRECT_PATHS = Arrays.asList(
            "/actuator",    
            "/index.html",
            "/static",
            "/favicon.ico",
            "/api/v1/groups"  // 允许服务间调用群组接口
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();
        String gatewayHeader = request.getHeader("X-Gateway-Service");
        String internalServiceHeader = request.getHeader("X-Internal-Service");

        // 允许直接访问的路径
        if (isAllowedDirectPath(requestPath)) {
            log.debug("允许直接访问: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // 允许来自网关的请求
        boolean fromGateway = "IM-Gateway".equals(gatewayHeader);
        // 允许来自内部服务的请求（如 IM-push-server）
        boolean fromInternalService = "IM-push-server".equals(internalServiceHeader);
        
        if (!fromGateway && !fromInternalService) {
            log.warn("拒绝直接访问: {}, gatewayHeader={}, internalServiceHeader={}", 
                requestPath, gatewayHeader, internalServiceHeader);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Direct access not allowed\",\"message\":\"Please access through gateway\",\"code\":403}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedDirectPath(String path) {
        return ALLOWED_DIRECT_PATHS.stream().anyMatch(path::startsWith);
    }
}
