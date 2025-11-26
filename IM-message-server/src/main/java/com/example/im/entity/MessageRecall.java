package com.example.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 消息撤回记录实体
 * 对应 im_db.message_recalls 表
 */
@Data
@Accessors(chain = true)
@TableName("message_recalls")
public class MessageRecall {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 消息ID */
    private Long messageId;
    
    /** 频道ID */
    private Long channelId;
    
    /** 原发送者ID */
    private Long senderId;
    
    /** 撤回操作者ID */
    private Long recallBy;
    
    /** 撤回原因 */
    private String recallReason;
    
    /** 原始内容（审计用）*/
    private String originalContent;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
