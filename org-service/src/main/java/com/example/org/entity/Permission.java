package com.example.org.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限实体
 * 对应 org_db.permissions 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "permissions", autoResultMap = true)
public class Permission {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 权限编码 */
    private String permissionCode;
    
    /** 权限名称 */
    private String name;
    
    /** 权限描述 */
    private String description;
    
    /** 模块：im, doc, file, admin */
    private String module;
    
    /** 资源：message, channel, document */
    private String resource;
    
    /** 操作：create, read, update, delete */
    private String action;
    
    /** 风险级别: 1低风险 2中风险 3高风险 */
    private Integer riskLevel;
    
    /** 依赖权限IDs */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> dependsOn;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 风险级别常量
    public static final int RISK_LOW = 1;
    public static final int RISK_MEDIUM = 2;
    public static final int RISK_HIGH = 3;
}
