package com.example.domain.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

/**
 * 群组节点
 */
@Node("Group")
@Data
public class GroupNode {
    
    @Id
    @GeneratedValue
    private Long id;
    
    private String groupId;  // 业务ID
    private String name;
    private String description;
    private String avatar;
    private Long ownerId;
    private Integer maxMembers;
    private String joinType;  // FREE, APPROVAL, INVITE_ONLY
    private String announcement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 成员数量（不持久化到数据库，由查询计算）
    private transient Integer memberCount;
}
