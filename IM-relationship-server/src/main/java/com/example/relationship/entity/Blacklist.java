package com.example.relationship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;

/**
 * 黑名单实体类
 */
@Data
@Accessors(chain = true)
@TableName("blacklist")
public class Blacklist {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户ID */
    private Long userId;
    
    /** 被拉黑用户ID */
    private Long blockedUserId;
    
    /** 拉黑原因 */
    private String reason;
    
    /** 屏蔽消息 */
    private Boolean blockMessages;
    
    /** 屏蔽通话 */
    private Boolean blockCalls;
    
    /** 屏蔽朋友圈 */
    private Boolean blockMoments;
    
    private OffsetDateTime createdAt;
}
