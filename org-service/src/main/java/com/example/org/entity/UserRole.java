package com.example.org.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户角色关联实体
 * 对应 org_db.user_roles 表
 */
@Data
@Accessors(chain = true)
@TableName("user_roles")
public class UserRole {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户ID */
    private Long userId;
    
    /** 角色ID */
    private Long roleId;
    
    /** 授权范围类型: global, org, dept, team */
    private String scopeType;
    
    /** 范围ID（组织/部门/团队ID）*/
    private Long scopeId;
    
    /** 生效日期 */
    private LocalDate effectiveDate;
    
    /** 过期日期 */
    private LocalDate expiryDate;
    
    /** 授权人ID */
    private Long grantedBy;
    
    /** 授权时间 */
    private LocalDateTime grantedAt;
    
    /** 撤销时间 */
    private LocalDateTime revokedAt;
    
    // 范围类型常量
    public static final String SCOPE_GLOBAL = "global";
    public static final String SCOPE_ORG = "org";
    public static final String SCOPE_DEPT = "dept";
    public static final String SCOPE_TEAM = "team";
}
