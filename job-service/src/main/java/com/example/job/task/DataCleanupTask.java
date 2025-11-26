package com.example.job.task;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据清理任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCleanupTask {
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 清理过期的好友申请
     * 每日凌晨2点执行
     */
    @XxlJob("cleanExpiredFriendRequests")
    public void cleanExpiredFriendRequests() {
        log.info("开始清理过期的好友申请...");
        
        String sql = """
            UPDATE friend_requests 
            SET status = 4 
            WHERE status = 0 AND expires_at < NOW()
            """;
        
        int count = jdbcTemplate.update(sql);
        log.info("过期好友申请清理完成，更新: {} 条", count);
    }
    
    /**
     * 清理过期的会话
     */
    @XxlJob("cleanExpiredSessions")
    public void cleanExpiredSessions() {
        log.info("开始清理过期会话...");
        
        String sql = """
            DELETE FROM user_sessions 
            WHERE expires_at < NOW()
            """;
        
        int count = jdbcTemplate.update(sql);
        log.info("过期会话清理完成，删除: {} 条", count);
    }
    
    /**
     * 清理过期的验证码
     */
    @XxlJob("cleanExpiredVerificationCodes")
    public void cleanExpiredVerificationCodes() {
        log.info("开始清理过期验证码...");
        
        String sql = """
            DELETE FROM verification_codes 
            WHERE expires_at < NOW()
            """;
        
        int count = jdbcTemplate.update(sql);
        log.info("过期验证码清理完成，删除: {} 条", count);
    }
    
    /**
     * 清理旧的审计日志（保留180天）
     */
    @XxlJob("cleanOldAuditLogs")
    public void cleanOldAuditLogs() {
        log.info("开始清理旧审计日志...");
        
        String sql = """
            DELETE FROM audit_logs 
            WHERE created_at < NOW() - INTERVAL '180 days'
            """;
        
        int count = jdbcTemplate.update(sql);
        log.info("旧审计日志清理完成，删除: {} 条", count);
    }
}
