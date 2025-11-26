package com.example.relationship.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 好友推荐实体
 * 对应 relationship_db.friend_recommendations 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "friend_recommendations", autoResultMap = true)
public class FriendRecommendation {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long recommendedUserId;
    
    /** 推荐原因: mutual_friends, same_group, nearby */
    private String reason;
    
    /** 推荐详情(JSONB) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> reasonDetail;
    
    /** 推荐分数 */
    private Double score;
    
    /** 用户反馈: 1-感兴趣, 2-不感兴趣, 3-已添加 */
    private Integer feedback;
    
    private OffsetDateTime feedbackAt;
    
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    
    private OffsetDateTime expiresAt;
    
    // 反馈类型常量
    public static final int FEEDBACK_INTERESTED = 1;
    public static final int FEEDBACK_NOT_INTERESTED = 2;
    public static final int FEEDBACK_ADDED = 3;
}
