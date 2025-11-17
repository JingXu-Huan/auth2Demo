package com.example.domain.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

/**
 * 用户加入群组关系
 */
@RelationshipProperties
@Data
public class MemberOfRelationship {
    
    @Id
    @GeneratedValue
    private Long id;
    
    private String role;  // OWNER, ADMIN, MEMBER
    private LocalDateTime joinedAt;
    private String nickname;  // 群昵称
    private Boolean muted;  // 是否禁言
    
    @TargetNode
    private GroupNode group;
}
