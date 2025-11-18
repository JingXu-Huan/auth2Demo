package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 群成员信息 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像
     */
    private String avatar;
    
    /**
     * 用户状态：ONLINE, OFFLINE
     */
    private String status;
    
    /**
     * 群内角色：OWNER(群主), ADMIN(管理员), MEMBER(成员)
     */
    private String role;
    
    /**
     * 加入时间
     */
    private String joinedAt;
    
    /**
     * 是否禁言
     */
    private Boolean muted;
    
    /**
     * 群昵称（备注）
     */
    private String groupNickname;
}
