package com.example.relationship.dto;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 好友申请VO
 */
@Data
public class FriendRequestVO {
    
    private Long id;
    private Long senderId;
    private Long receiverId;
    
    /** 发送者昵称 */
    private String senderNickname;
    
    /** 发送者头像 */
    private String senderAvatar;
    
    /** 申请消息 */
    private String message;
    
    /** 备注名 */
    private String remark;
    
    /** 状态 */
    private Integer status;
    
    /** 状态文本 */
    private String statusText;
    
    /** 来源 */
    private String source;
    
    /** 过期时间 */
    private OffsetDateTime expiresAt;
    
    private OffsetDateTime createdAt;
    
    /** 是否已过期 */
    private Boolean expired;
}
