package com.example.org.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 角色实体
 * 对应 org_db.roles 表
 */
@Data
@Accessors(chain = true)
@TableName("roles")
public class Role {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 组织ID (NULL表示系统角色) */
    private Long orgId;
    
    /** 角色编码 */
    private String roleCode;
    
    /** 角色名称 */
    private String name;
    
    /** 角色描述 */
    private String description;
    
    /** 角色类型: 1自定义 2系统预设 3部门角色 */
    private Integer roleType;
    
    /** 角色级别（用于权限继承）*/
    private Integer roleLevel;
    
    /** 父角色ID */
    private Long parentRoleId;
    
    /** 是否默认角色 */
    private Boolean isDefault;
    
    /** 最大成员数 */
    private Integer maxMembers;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    // 角色类型常量
    public static final int TYPE_CUSTOM = 1;
    public static final int TYPE_SYSTEM = 2;
    public static final int TYPE_DEPT = 3;
}
