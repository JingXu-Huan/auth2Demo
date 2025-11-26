package com.example.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 用户详情实体
 * 对应 user_profiles 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "user_profiles", autoResultMap = true)
public class UserProfile implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID（主键，关联users表）
     */
    @TableId(type = IdType.INPUT)
    private Long userId;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 性别：1-男，2-女，0-未知
     */
    private Integer gender;
    
    /**
     * 生日
     */
    private LocalDate birthday;
    
    /**
     * 公司
     */
    private String company;
    
    /**
     * 部门
     */
    private String department;
    
    /**
     * 职位
     */
    private String position;
    
    /**
     * 员工编号
     */
    private String employeeId;
    
    /**
     * 入职日期
     */
    private LocalDate hireDate;
    
    /**
     * 国家
     */
    private String country;
    
    /**
     * 省份
     */
    private String province;
    
    /**
     * 城市
     */
    private String city;
    
    /**
     * 详细地址
     */
    private String address;
    
    /**
     * 个人简介
     */
    private String bio;
    
    /**
     * 时区
     */
    private String timezone;
    
    /**
     * 语言偏好
     */
    private String locale;
    
    /**
     * 扩展字段（JSONB）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extra;
    
    /**
     * 更新时间
     */
    private OffsetDateTime updatedAt;
}
