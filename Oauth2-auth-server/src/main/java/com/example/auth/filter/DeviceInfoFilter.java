package com.example.auth.filter;

import com.example.auth.context.DeviceContextHolder;
import com.example.auth.dto.DeviceInfo;
import com.example.auth.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 设备信息过滤器
 * 解析请求中的设备信息并存入ThreadLocal
 * 
 * @author Cascade AI
 * @date 2024-11-24
 */
@Slf4j
@Component
public class DeviceInfoFilter extends OncePerRequestFilter {
    
    @Autowired
    private DeviceService deviceService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            // 从Header中获取设备信息
            String deviceId = request.getHeader("X-Device-Id");
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = getClientIP(request);
            
            // 解析设备信息
            DeviceInfo deviceInfo = deviceService.parseDeviceInfo(deviceId, ipAddress, userAgent);
            
            // 存入ThreadLocal
            DeviceContextHolder.setDevice(deviceInfo);
            
            log.debug("设备信息已解析: deviceId={}, type={}, ip={}", 
                deviceInfo.getDeviceId(), deviceInfo.getDeviceType(), ipAddress);
            
            // 继续过滤链
            filterChain.doFilter(request, response);
            
        } finally {
            // 请求结束后清理ThreadLocal，防止内存泄漏
            DeviceContextHolder.clear();
        }
    }
    
    /**
     * 获取客户端真实IP
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理多个IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}
