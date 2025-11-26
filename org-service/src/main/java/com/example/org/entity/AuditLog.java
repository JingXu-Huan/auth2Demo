package com.example.org.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作审计日志实体
 * 对应 org_db.audit_logs 表（分区表）
 */
@Data
@Accessors(chain = true)
@TableName(value = "audit_logs", autoResultMap = true)
public class AuditLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 操作者用户ID */
    private Long userId;
    
    /** 组织ID */
    private Long orgId;
    
    /** 操作类型 */
    private String action;
    
    /** 资源类型 */
    private String resourceType;
    
    /** 资源ID */
    private String resourceId;
    
    /** 操作详情 (JSONB) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> details;
    
    /** 变更内容 (JSONB) {"before": {}, "after": {}} */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> changes;
    
    /** 操作是否成功 */
    private Boolean success;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** IP地址 */
    private String ipAddress;
    
    /** User-Agent */
    private String userAgent;
    
    /** 请求ID */
    private String requestId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
