package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 好友信息 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendDTO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 状态：ONLINE, OFFLINE
     */
    private String status;
    
    /**
     * 备注名
     */
    private String remark;
    
    /**
     * 成为好友的时间
     */
    private String createdAt;
}
