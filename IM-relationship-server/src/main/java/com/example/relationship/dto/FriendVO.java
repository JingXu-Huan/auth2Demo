package com.example.relationship.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 好友信息VO
 */
@Data
public class FriendVO {
    
    private Long userId;
    private Long friendId;
    
    /** 好友昵称（从用户服务获取） */
    private String nickname;
    
    /** 好友头像 */
    private String avatar;
    
    /** 备注名 */
    private String remark;
    
    /** 标签 */
    private List<String> tags;
    
    /** 分组ID */
    private Long groupId;
    
    /** 分组名称 */
    private String groupName;
    
    /** 是否星标 */
    private Boolean starred;
    
    /** 是否拉黑 */
    private Boolean blocked;
    
    /** 关系类型 */
    private Integer relationshipType;
    
    /** 亲密度 */
    private Integer intimacyScore;
    
    /** 在线状态 */
    private Boolean online;
    
    /** 成为好友时间 */
    private OffsetDateTime createdAt;
}
