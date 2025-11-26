package com.example.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 管理员实体
 */
@Data
@Accessors(chain = true)
@TableName("admins")
public class Admin {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户名 */
    private String username;
    
    /** 密码(BCrypt) */
    private String password;
    
    /** 真实姓名 */
    private String realName;
    
    /** 角色: SUPER_ADMIN, AUDITOR, IT_ADMIN, OPERATOR */
    private String role;
    
    /** 状态: 1启用 0禁用 */
    private Integer status;
    
    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;
    
    private LocalDateTime createdAt;
    
    // 角色常量
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_AUDITOR = "AUDITOR";
    public static final String ROLE_IT_ADMIN = "IT_ADMIN";
    public static final String ROLE_OPERATOR = "OPERATOR";
}
