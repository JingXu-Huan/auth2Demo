package com.example.relationship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;

/**
 * 好友分组实体类
 */
@Data
@Accessors(chain = true)
@TableName("friend_groups")
public class FriendGroup {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户ID */
    private Long userId;
    
    /** 分组名称 */
    private String name;
    
    /** 是否默认分组 */
    private Boolean isDefault;
    
    /** 排序 */
    private Integer sortOrder;
    
    /** 成员数量 */
    private Integer memberCount;
    
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
