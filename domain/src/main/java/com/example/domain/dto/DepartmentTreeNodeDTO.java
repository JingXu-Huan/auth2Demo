package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点 DTO，用于返回组织架构树
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentTreeNodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点ID：部门使用 deptId，成员使用 userId 字符串
     */
    private String id;

    /**
     * 节点名称：部门名称或成员姓名
     */
    private String name;

    /**
     * 节点类型：org / dept / user
     */
    private String type;

    /**
     * 头像（成员或组织 Logo）
     */
    private String avatar;

    /**
     * 成员所属部门名称，仅对 user 有意义
     */
    private String department;

    /**
     * 成员职位，仅对 user 有意义
     */
    private String position;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 员工编号
     */
    private String employeeId;

    /**
     * 子节点
     */
    private List<DepartmentTreeNodeDTO> children = new ArrayList<>();
}
