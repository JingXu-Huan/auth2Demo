package com.example.async.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表（事务消息）
 */
@Data
@TableName("local_messages")
public class LocalMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息唯一ID
     */
    private String messageId;

    /**
     * 消息主题
     */
    private String topic;

    /**
     * 消息标签
     */
    private String tag;

    /**
     * 消息键
     */
    private String keys;

    /**
     * 消息体（JSON）
     */
    private String payload;

    /**
     * 业务唯一键
     */
    private String businessKey;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 分片键
     */
    private String shardingKey;

    /**
     * 状态：0-待发送, 1-已发送, 2-已确认, 3-发送失败, 4-已取消
     */
    private Integer status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetry;

    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryAt;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime failedAt;
}
