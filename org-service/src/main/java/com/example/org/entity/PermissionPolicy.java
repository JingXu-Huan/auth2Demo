package com.example.org.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 权限策略实体
 * 对应 org_db.permission_policies 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "permission_policies", autoResultMap = true)
public class PermissionPolicy {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 策略名称 */
    private String name;

    /** 策略描述 */
    private String description;

    /** 策略效果: allow, deny */
    private String effect;

    /** 权限主体: {"users": [], "roles": [], "departments": []} */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> principals;

    /** 资源: {"types": [], "ids": [], "tags": []} */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> resources;

    /** 操作: ["read", "write", "delete"] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> actions;

    /** 条件: {"ip_range": "...", "time_range": {...}} */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> conditions;

    /** 优先级（数字越小优先级越高） */
    private Integer priority;

    /** 是否启用 */
    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
