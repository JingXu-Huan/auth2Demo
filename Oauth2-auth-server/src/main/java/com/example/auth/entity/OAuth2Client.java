package com.example.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OAuth2客户端实体
 * 对应 auth_db.oauth2_clients 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "oauth2_clients", autoResultMap = true)
public class OAuth2Client {
    
    /** 客户端ID */
    @TableId(type = IdType.INPUT)
    private String id;
    
    /** 客户端密钥 */
    private String clientSecret;
    
    /** 客户端名称 */
    private String clientName;
    
    /** 重定向URI列表 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> redirectUris;
    
    /** 授权类型 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> grantTypes;
    
    /** 响应类型 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> responseTypes;
    
    /** 权限范围 */
    private String scope;
    
    /** 访问令牌有效期(秒) */
    private Integer accessTokenValidity;
    
    /** 刷新令牌有效期(秒) */
    private Integer refreshTokenValidity;
    
    /** 是否自动授权 */
    private Boolean autoApprove;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
