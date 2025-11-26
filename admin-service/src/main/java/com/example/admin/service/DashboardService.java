package com.example.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 仪表盘服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final StringRedisTemplate redisTemplate;
    private final ReportService reportService;
    
    /**
     * 获取仪表盘统计数据
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 在线用户数（从Redis读取）
        String onlineCount = redisTemplate.opsForValue().get("stats:online_users");
        stats.put("onlineUsers", onlineCount != null ? Long.parseLong(onlineCount) : 0);
        
        // 今日消息数
        String todayMsgCount = redisTemplate.opsForValue().get("stats:today_messages");
        stats.put("todayMessages", todayMsgCount != null ? Long.parseLong(todayMsgCount) : 0);
        
        // 待处理举报数
        stats.put("pendingReports", reportService.countPending());
        
        // 注册用户数（从Redis读取）
        String totalUsers = redisTemplate.opsForValue().get("stats:total_users");
        stats.put("totalUsers", totalUsers != null ? Long.parseLong(totalUsers) : 0);
        
        return stats;
    }
}
