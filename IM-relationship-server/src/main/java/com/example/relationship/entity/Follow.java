package com.example.relationship.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;

/**
 * 关注关系实体
 * 对应 relationship_db.follows 表
 */
@Data
@Accessors(chain = true)
@TableName("follows")
public class Follow {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 关注者ID */
    private Long followerId;
    
    /** 被关注者ID */
    private Long followingId;
    
    /** 是否特别关注 */
    private Boolean specialFollow;
    
    /** 是否开启通知 */
    private Boolean notificationsEnabled;
    
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
