package com.example.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 离线消息队列实体
 * 对应 im_db.offline_message_queue 表
 */
@Data
@Accessors(chain = true)
@TableName("offline_message_queue")
public class OfflineMessageQueue {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户ID */
    private Long userId;
    
    /** 消息ID */
    private Long messageId;
    
    /** 频道ID */
    private Long channelId;
    
    /** 优先级 */
    private Integer priority;
    
    /** 重试次数 */
    private Integer retryCount;
    
    /** 最大重试次数 */
    private Integer maxRetry;
    
    /** 下次重试时间 */
    private LocalDateTime nextRetryAt;
    
    /** 是否已投递 */
    private Boolean delivered;
    
    /** 投递时间 */
    private LocalDateTime deliveredAt;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
