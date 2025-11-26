package com.example.relationship.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 好友申请请求
 */
@Data
public class FriendApplyRequest {
    
    @NotNull(message = "目标用户ID不能为空")
    private Long targetUserId;
    
    /** 验证消息 */
    private String message;
    
    /** 备注名 */
    private String remark;
    
    /** 来源：search, qrcode, group, recommend, card */
    private String source;
    
    /** 来源ID */
    private String sourceId;
}
