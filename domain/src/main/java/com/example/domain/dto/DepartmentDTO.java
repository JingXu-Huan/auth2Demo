package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门信息 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String deptId;

    private String orgId;

    private String name;

    private String fullPathName;

    private String parentDeptId;

    private Integer level;

    private Integer sortOrder;

    private Long leaderUserId;

    private Boolean deleted;

    private Integer memberCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
