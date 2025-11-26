package com.example.auth.service;

import com.example.domain.model.LoginLog;
import com.example.auth.mapper.LoginLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 登录日志服务
 * 记录用户登录历史，用于安全审计和异常检测
 */
@Slf4j
@Service
public class LoginLogService {
    
    @Autowired
    private LoginLogMapper loginLogMapper;
    
    /**
     * 异步记录登录日志
     */
    @Async
    public void recordLoginLog(Long userId, String loginType, boolean success,
                               HttpServletRequest request, String errorMessage) {
        try {
            LoginLog loginLog = new LoginLog();
            loginLog.setUserId(userId);
            loginLog.setLoginType(loginType);
            loginLog.setSuccess(success);
            loginLog.setIpAddress(getClientIp(request));
            loginLog.setUserAgent(request.getHeader("User-Agent"));
            loginLog.setDeviceId(request.getHeader("X-Device-Id"));
            loginLog.setErrorMessage(errorMessage);
            loginLog.setCreatedAt(LocalDateTime.now());
            
            // 解析地理位置（可选，可以调用IP库服务）
            // loginLog.setCountry(parseCountry(ipAddress));
            // loginLog.setCity(parseCity(ipAddress));
            
            loginLogMapper.insert(loginLog);
            
            if (success) {
                log.info("登录成功日志: userId={}, loginType={}, ip={}", 
                    userId, loginType, loginLog.getIpAddress());
            } else {
                log.warn("登录失败日志: userId={}, loginType={}, ip={}, error={}", 
                    userId, loginType, loginLog.getIpAddress(), errorMessage);
            }
            
        } catch (Exception e) {
            log.error("记录登录日志失败: userId={}, loginType={}", userId, loginType, e);
        }
    }
    
    /**
     * 记录简单登录日志
     */
    @Async
    public void recordLoginLog(Long userId, String loginType, boolean success,
                               String ipAddress, String userAgent) {
        try {
            LoginLog loginLog = new LoginLog();
            loginLog.setUserId(userId);
            loginLog.setLoginType(loginType);
            loginLog.setSuccess(success);
            loginLog.setIpAddress(ipAddress);
            loginLog.setUserAgent(userAgent);
            loginLog.setCreatedAt(LocalDateTime.now());
            
            loginLogMapper.insert(loginLog);
            
        } catch (Exception e) {
            log.error("记录登录日志失败: userId={}, loginType={}", userId, loginType, e);
        }
    }
    
    /**
     * 获取用户最近的登录记录
     */
    public LoginLog getLastLoginLog(Long userId) {
        try {
            return loginLogMapper.getLastLoginLog(userId);
        } catch (Exception e) {
            log.error("获取最后登录记录失败: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 检查是否有异常登录（如异地登录）
     */
    public boolean checkAbnormalLogin(Long userId, String currentIp) {
        try {
            // 获取最近的成功登录记录
            LoginLog lastLogin = loginLogMapper.getLastSuccessLoginLog(userId);
            
            if (lastLogin == null) {
                return false; // 首次登录
            }
            
            // 简单的IP段检查（实际应该使用IP地理位置服务）
            if (!isSameIpRange(lastLogin.getIpAddress(), currentIp)) {
                log.warn("检测到异地登录: userId={}, lastIp={}, currentIp={}", 
                    userId, lastLogin.getIpAddress(), currentIp);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("检查异常登录失败: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 获取登录失败次数
     */
    public int getFailedLoginCount(Long userId, int minutes) {
        try {
            LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
            return loginLogMapper.countFailedLogins(userId, since);
        } catch (Exception e) {
            log.error("获取登录失败次数失败: userId={}", userId, e);
            return 0;
        }
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 简单的IP段比较
     */
    private boolean isSameIpRange(String ip1, String ip2) {
        if (ip1 == null || ip2 == null) {
            return false;
        }
        
        String[] parts1 = ip1.split("\\.");
        String[] parts2 = ip2.split("\\.");
        
        if (parts1.length != 4 || parts2.length != 4) {
            return false;
        }
        
        // 比较前三段
        return parts1[0].equals(parts2[0]) && 
               parts1[1].equals(parts2[1]) && 
               parts1[2].equals(parts2[2]);
    }
}
