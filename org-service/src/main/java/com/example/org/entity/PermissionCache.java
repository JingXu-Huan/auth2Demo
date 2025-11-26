package com.example.org.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 权限缓存实体
 * 对应 org_db.permission_cache 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "permission_cache", autoResultMap = true)
public class PermissionCache {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 缓存的权限代码数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> permissions;

    /** 角色ID数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> roles;

    /** 数据权限范围 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> dataScopes;

    /** 计算时间 */
    private LocalDateTime calculatedAt;

    /** 过期时间 */
    private LocalDateTime expiresAt;
}
