package org.example.imserver.filter;

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
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();
        String gatewayHeader = request.getHeader("X-Gateway-Service");

        if (isAllowedDirectPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean fromGateway = "IM-Gateway".equals(gatewayHeader);
        if (!fromGateway) {
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
