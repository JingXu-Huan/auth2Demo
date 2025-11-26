package com.example.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 举报/投诉记录实体
 */
@Data
@Accessors(chain = true)
@TableName("reports")
public class Report {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 举报人ID */
    private Long reporterId;
    
    /** 举报目标类型: MSG, USER, DOC, GROUP */
    private String targetType;
    
    /** 目标ID */
    private String targetId;
    
    /** 举报原因 */
    private String reason;
    
    /** 状态: 0待处理 1已封禁 2已忽略 */
    private Integer status;
    
    /** 管理员处理意见 */
    private String adminComment;
    
    /** 处理人 */
    private Long handledBy;
    
    private LocalDateTime createdAt;
    
    // 状态常量
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_BANNED = 1;
    public static final int STATUS_IGNORED = 2;
}
