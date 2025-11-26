package com.example.async.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务表
 */
@Data
@TableName("scheduled_tasks")
public class ScheduledTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务类型：cron, delay, rate
     */
    private String taskType;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 延迟秒数
     */
    private Integer delaySeconds;

    /**
     * 执行频率（秒）
     */
    private Integer rateSeconds;

    /**
     * 处理器类
     */
    private String handlerClass;

    /**
     * 任务参数（JSON）
     */
    private String parameters;

    /**
     * 最大重试次数
     */
    private Integer maxRetry;

    /**
     * 超时时间（秒）
     */
    private Integer timeoutSeconds;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 上次执行时间
     */
    private LocalDateTime lastRunAt;

    /**
     * 上次执行状态
     */
    private String lastRunStatus;

    /**
     * 上次执行消息
     */
    private String lastRunMessage;

    /**
     * 下次执行时间
     */
    private LocalDateTime nextRunAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
