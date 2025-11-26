package com.example.org.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点DTO
 */
@Data
public class DepartmentNode {
    
    private Long id;
    private String name;
    private Long parentId;
    private String path;
    private Integer level;
    private Integer sortOrder;
    private Long managerId;
    private String managerName;
    private Integer memberCount;
    
    /** 子部门列表 */
    private List<DepartmentNode> children = new ArrayList<>();
}
