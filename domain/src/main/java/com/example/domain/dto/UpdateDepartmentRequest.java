package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新部门请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDepartmentRequest {

    private String name;

    private String parentDeptId;

    private Integer sortOrder;

    private Long leaderUserId;

    private Boolean deleted;
}
