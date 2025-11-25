package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 添加好友请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddFriendRequest {
    
    /**
     * 发起者用户ID
     */
    @NotNull(message = "发起者ID不能为空")
    private Long fromUserId;
    
    /**
     * 目标用户ID
     */
    @NotNull(message = "目标用户ID不能为空")
    private Long toUserId;
    
    /**
     * 验证消息
     */
    private String message;
    
    /**
     * 备注名
     */
    private String remark;
}
