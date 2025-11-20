package com.example.domain.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

@Node("Department")
@Data
public class DepartmentNode {

    @Id
    @GeneratedValue
    private Long id;

    // 业务部门ID，对外使用
    private String deptId;

    // 所属组织 orgId
    private String orgId;

    // 部门名称
    private String name;

    // 部门完整路径名称，例如 "Lantis/技术中心/IM事业部"
    private String fullPathName;

    // 父部门ID，根部门为 null
    private String parentDeptId;

    // 层级，从 1 开始
    private Integer level;

    // 排序字段，越小越靠前
    private Integer sortOrder;

    // 负责人用户ID
    private Long leaderUserId;

    // 是否已删除（做软删除预留）
    private Boolean deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 成员数量（不持久化，由服务查询后填充）
    private transient Integer memberCount;
}
