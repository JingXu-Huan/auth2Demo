package com.example.org.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据权限实体（行级权限）
 * 对应 org_db.data_permissions 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "data_permissions", autoResultMap = true)
public class DataPermission {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 权限主体类型: user, role, dept */
    private String principalType;
    
    /** 权限主体ID */
    private Long principalId;
    
    /** 资源类型 */
    private String resourceType;
    
    /** 范围类型: all, org, dept, self, custom */
    private String scopeType;
    
    /** 自定义范围条件 (JSONB) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> scopeValue;
    
    /** 是否可读 */
    private Boolean canRead;
    
    /** 是否可写 */
    private Boolean canWrite;
    
    /** 是否可删除 */
    private Boolean canDelete;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 主体类型常量
    public static final String PRINCIPAL_USER = "user";
    public static final String PRINCIPAL_ROLE = "role";
    public static final String PRINCIPAL_DEPT = "dept";
    
    // 范围类型常量
    public static final String SCOPE_ALL = "all";
    public static final String SCOPE_ORG = "org";
    public static final String SCOPE_DEPT = "dept";
    public static final String SCOPE_SELF = "self";
    public static final String SCOPE_CUSTOM = "custom";
}
