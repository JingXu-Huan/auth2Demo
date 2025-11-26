package com.example.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 消息已读记录实体
 * 对应 im_db.message_reads 表
 */
@Data
@Accessors(chain = true)
@TableName("message_reads")
public class MessageRead {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 消息ID */
    private Long messageId;
    
    /** 频道ID */
    private Long channelId;
    
    /** 用户ID */
    private Long userId;
    
    /** 阅读时间 */
    private LocalDateTime readAt;
}
