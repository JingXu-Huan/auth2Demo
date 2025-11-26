package com.example.org.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 组织实体（多租户）
 * 对应 org_db.organizations 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "organizations", autoResultMap = true)
public class Organization {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 组织代码 */
    private String orgCode;
    
    /** 组织名称 */
    private String name;
    
    /** 组织全称 */
    private String fullName;
    
    /** 组织类型: company, school, government, ngo */
    private String orgType;
    
    /** 行业 */
    private String industry;
    
    /** 员工数量 */
    private Integer employeeCount;
    
    /** 最大成员数 */
    private Integer maxMembers;
    
    /** 联系邮箱 */
    private String contactEmail;
    
    /** 联系电话 */
    private String contactPhone;
    
    /** 网站 */
    private String website;
    
    /** 国家 */
    private String country;
    
    /** 省份 */
    private String province;
    
    /** 城市 */
    private String city;
    
    /** 详细地址 */
    private String address;
    
    /** 是否认证 */
    private Boolean isVerified;
    
    /** 认证时间 */
    private LocalDateTime verifiedAt;
    
    /** 营业执照号 */
    private String businessLicense;
    
    /** 功能开关 (JSONB) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> features;
    
    /** 组织设置 (JSONB) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> settings;
    
    /** Logo URL */
    private String logoUrl;
    
    /** 状态: 1正常 2冻结 3注销 */
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private LocalDateTime deletedAt;
    
    // 状态常量
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_FROZEN = 2;
    public static final int STATUS_CANCELLED = 3;
}
