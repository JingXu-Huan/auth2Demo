package com.example.org.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 角色权限关联实体
 * 对应 org_db.role_permissions 表
 */
@Data
@Accessors(chain = true)
@TableName("role_permissions")
public class RolePermission {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 角色ID */
    private Long roleId;
    
    /** 权限ID */
    private Long permissionId;
    
    /** 授权人ID */
    private Long grantedBy;
    
    /** 授权时间 */
    private LocalDateTime grantedAt;
}
