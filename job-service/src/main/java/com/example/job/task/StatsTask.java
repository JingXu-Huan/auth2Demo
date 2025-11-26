package com.example.job.task;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 统计任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsTask {
    
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 统计昨日消息量
     * 每日凌晨1点执行
     */
    @XxlJob("calcDailyMessageStats")
    public void calcDailyMessageStats() {
        log.info("开始统计昨日消息量...");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String tableName = "chat_messages_" + yesterday.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        
        try {
            String sql = String.format("""
                SELECT COUNT(*) FROM %s 
                WHERE created_at >= '%s' AND created_at < '%s'
                """, tableName, yesterday, yesterday.plusDays(1));
            
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            
            // 存入Redis
            String key = "stats:daily_messages:" + yesterday;
            redisTemplate.opsForValue().set(key, String.valueOf(count), 90, TimeUnit.DAYS);
            
            log.info("昨日消息统计完成: {} 条", count);
        } catch (Exception e) {
            log.error("统计昨日消息量失败", e);
        }
    }
    
    /**
     * 统计活跃用户数
     */
    @XxlJob("calcDailyActiveUsers")
    public void calcDailyActiveUsers() {
        log.info("开始统计昨日活跃用户...");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        try {
            String sql = """
                SELECT COUNT(DISTINCT user_id) FROM login_logs 
                WHERE created_at >= ? AND created_at < ?
                """;
            
            Long count = jdbcTemplate.queryForObject(sql, Long.class, yesterday, yesterday.plusDays(1));
            
            String key = "stats:daily_active_users:" + yesterday;
            redisTemplate.opsForValue().set(key, String.valueOf(count), 90, TimeUnit.DAYS);
            
            log.info("昨日活跃用户统计完成: {} 人", count);
        } catch (Exception e) {
            log.error("统计活跃用户失败", e);
        }
    }
    
    /**
     * 更新总用户数缓存
     */
    @XxlJob("updateTotalUsersCache")
    public void updateTotalUsersCache() {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE status = 1", Long.class);
            redisTemplate.opsForValue().set("stats:total_users", String.valueOf(count));
            log.info("更新总用户数缓存: {}", count);
        } catch (Exception e) {
            log.error("更新总用户数缓存失败", e);
        }
    }
}
