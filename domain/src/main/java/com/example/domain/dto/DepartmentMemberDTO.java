package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 部门成员 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentMemberDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 成员姓名（对外展示名称）
     */
    private String name;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 所属部门名称（当前部门）
     */
    private String department;

    /**
     * 职位/头衔
     */
    private String title;

    /**
     * 是否主部门
     */
    private Boolean primaryDepartment;

    /**
     * 部门内排序
     */
    private Integer sortOrder;

    /**
     * 加入时间（字符串格式）
     */
    private String joinedAt;

    /**
     * 邮箱（如有）
     */
    private String email;

    /**
     * 手机号（如有）
     */
    private String phone;

    /**
     * 员工编号（如有）
     */
    private String employeeId;

    /**
     * 在线状态
     */
    private String status;
}
