package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量添加部门成员请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddDepartmentMembersRequest {

    /**
     * 要添加的用户ID列表
     */
    private List<Long> userIds;

    /**
     * 是否设为主部门
     */
    private Boolean primaryDepartment;

    /**
     * 默认职位/头衔
     */
    private String title;
}
