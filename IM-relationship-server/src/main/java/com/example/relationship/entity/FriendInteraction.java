package com.example.relationship.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;

/**
 * 好友互动记录实体
 * 对应 relationship_db.friend_interactions 表
 */
@Data
@Accessors(chain = true)
@TableName("friend_interactions")
public class FriendInteraction {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long friendId;
    
    /** 消息数 */
    private Integer messageCount;
    
    /** 通话次数 */
    private Integer callCount;
    
    /** 总通话时长(秒) */
    private Integer callDuration;
    
    /** 最近消息时间 */
    private OffsetDateTime lastMessageAt;
    
    /** 最近通话时间 */
    private OffsetDateTime lastCallAt;
    
    /** 最近互动时间 */
    private OffsetDateTime lastInteractionAt;
    
    /** 互动分数（亲密度） */
    private Integer interactionScore;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
