package com.example.job.task;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 分区管理任务
 * 自动创建PostgreSQL分区表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartitionManageTask {
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 创建下个月的消息分区表
     * 每月25号执行
     */
    @XxlJob("createMessagePartition")
    public void createMessagePartition() {
        LocalDate nextMonth = LocalDate.now().plusMonths(1);
        String partitionName = "chat_messages_" + nextMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String startDate = nextMonth.withDayOfMonth(1).toString();
        String endDate = nextMonth.plusMonths(1).withDayOfMonth(1).toString();
        
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s PARTITION OF chat_messages
            FOR VALUES FROM ('%s') TO ('%s')
            """, partitionName, startDate, endDate);
        
        try {
            jdbcTemplate.execute(sql);
            log.info("创建分区表成功: {}", partitionName);
        } catch (Exception e) {
            log.error("创建分区表失败: {}", partitionName, e);
        }
    }
    
    /**
     * 创建下个月的登录日志分区表
     */
    @XxlJob("createLoginLogPartition")
    public void createLoginLogPartition() {
        LocalDate nextMonth = LocalDate.now().plusMonths(1);
        String partitionName = "login_logs_" + nextMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String startDate = nextMonth.withDayOfMonth(1).toString();
        String endDate = nextMonth.plusMonths(1).withDayOfMonth(1).toString();
        
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s PARTITION OF login_logs
            FOR VALUES FROM ('%s') TO ('%s')
            """, partitionName, startDate, endDate);
        
        try {
            jdbcTemplate.execute(sql);
            log.info("创建登录日志分区表成功: {}", partitionName);
        } catch (Exception e) {
            log.error("创建登录日志分区表失败: {}", partitionName, e);
        }
    }
}
