package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 好友请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestDTO {
    
    /**
     * 请求ID（关系ID）
     */
    private Long requestId;
    
    /**
     * 发送者用户ID
     */
    private Long fromUserId;
    
    /**
     * 发送者昵称
     */
    private String fromNickname;
    
    /**
     * 发送者头像
     */
    private String fromAvatar;
    
    /**
     * 接收者用户ID
     */
    private Long toUserId;
    
    /**
     * 验证消息
     */
    private String message;
    
    /**
     * 请求状态：PENDING(待处理), ACCEPTED(已接受), REJECTED(已拒绝)
     */
    private String status;
    
    /**
     * 创建时间
     */
    private String createdAt;
}
