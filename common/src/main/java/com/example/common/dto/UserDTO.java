package com.example.common.dto;

import com.example.common.model.User;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 用户数据传输对象（安全版本，不包含敏感信息）
 */
@Data
public class UserDTO {
    
    private Long id;
    
    private String username;
    
    private String displayName;
    
    private String email;
    
    private Boolean emailVerified;
    
    private String avatarUrl;
    
    private LocalDateTime createdAt;
    
    // 不包含以下敏感字段：
    // - confirmationToken
    // - tokenExpiry
    // - lastLoginAt (可能暴露用户活跃度)
    
    /**
     * 从 User 实体转换为 UserDTO
     */
    public static UserDTO fromUser(User user) {
        if (user == null) {
            return null;
        }
        
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getDisplayName());
        dto.setEmail(user.getEmail());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setCreatedAt(user.getCreatedAt());
        
        return dto;
    }
}
