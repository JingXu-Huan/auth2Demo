package com.example.relationship.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 好友申请审核请求
 */
@Data
public class FriendAuditRequest {
    
    @NotNull(message = "申请ID不能为空")
    private Long requestId;
    
    /** 操作：ACCEPT, REJECT, IGNORE */
    @NotNull(message = "操作类型不能为空")
    private String action;
    
    /** 拒绝原因 */
    private String rejectReason;
    
    /** 备注名（同意时设置） */
    private String remark;
    
    /** 分组ID（同意时设置） */
    private Long groupId;
    
    // 操作常量
    public static final String ACTION_ACCEPT = "ACCEPT";
    public static final String ACTION_REJECT = "REJECT";
    public static final String ACTION_IGNORE = "IGNORE";
}
