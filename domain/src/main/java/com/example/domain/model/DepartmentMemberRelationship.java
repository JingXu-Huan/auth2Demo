package com.example.domain.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

@RelationshipProperties
@Data
public class DepartmentMemberRelationship {

    @Id
    @GeneratedValue
    private Long id;

    // 是否主部门
    private Boolean primaryDepartment;

    // 职位/头衔
    private String title;

    // 排序字段（用于部门成员列表排序）
    private Integer sortOrder;

    private LocalDateTime joinedAt;

    @TargetNode
    private DepartmentNode department;
}
