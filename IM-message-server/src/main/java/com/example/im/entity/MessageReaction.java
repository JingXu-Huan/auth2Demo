package com.example.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 消息反应（表情回复）实体
 * 对应 im_db.message_reactions 表
 */
@Data
@Accessors(chain = true)
@TableName("message_reactions")
public class MessageReaction {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 消息ID */
    private Long messageId;
    
    /** 用户ID */
    private Long userId;
    
    /** Emoji符号 */
    private String emoji;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
