package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建部门请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDepartmentRequest {

    /**
     * 所属组织ID，不传则使用默认组织
     */
    private String orgId;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 父部门业务ID，根部门为 null
     */
    private String parentDeptId;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 部门负责人用户ID
     */
    private Long leaderUserId;
}
