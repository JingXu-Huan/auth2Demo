package com.example.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志实体
 * 对应 login_logs 分区表
 */
@Data
@Accessors(chain = true)
@TableName("login_logs")
public class LoginLog implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 登录类型：password, oauth, sso, mfa
     */
    private String loginType;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * User-Agent
     */
    private String userAgent;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * 国家
     */
    private String country;
    
    /**
     * 城市
     */
    private String city;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建时间（分区键）
     */
    private LocalDateTime createdAt;
}
