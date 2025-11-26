package com.example.relationship.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 共同好友缓存实体
 * 对应 relationship_db.mutual_friends_cache 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "mutual_friends_cache", autoResultMap = true)
public class MutualFriendsCache {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long user1Id;
    
    private Long user2Id;
    
    /** 共同好友ID列表 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> mutualFriendIds;
    
    /** 共同好友数量 */
    private Integer mutualCount;
    
    /** 计算时间 */
    private OffsetDateTime calculatedAt;
    
    /** 过期时间 */
    private OffsetDateTime expiresAt;
}
