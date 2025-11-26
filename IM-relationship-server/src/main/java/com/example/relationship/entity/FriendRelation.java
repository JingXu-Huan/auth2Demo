package com.example.relationship.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 好友关系实体类
 */
@Data
@Accessors(chain = true)
@TableName(value = "friend_relations", autoResultMap = true)
public class FriendRelation {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户ID */
    private Long userId;
    
    /** 好友ID */
    private Long friendId;
    
    /** 备注名 */
    private String remark;
    
    /** 标签 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;
    
    /** 描述 */
    private String description;
    
    /** 分组ID */
    private Long groupId;
    
    /** 是否拉黑 */
    private Boolean blocked;
    
    /** 是否星标好友 */
    private Boolean starred;
    
    /** 是否对其隐身 */
    private Boolean stealth;
    
    /** 允许查看朋友圈 */
    private Boolean allowViewMoments;
    
    /** 允许查看在线状态 */
    private Boolean allowViewOnline;
    
    /** 关系类型：1:普通好友, 2:亲密好友, 3:关注 */
    private Integer relationshipType;
    
    /** 亲密度分数 */
    private Integer intimacyScore;
    
    /** 来源 */
    private String source;
    
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // 注意：不使用 @TableLogic，因为 MyBatis-Plus 不支持 TIMESTAMPTZ 类型的逻辑删除
    private OffsetDateTime deletedAt;
    
    // 关系类型常量
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_CLOSE = 2;
    public static final int TYPE_FOLLOW = 3;
}
