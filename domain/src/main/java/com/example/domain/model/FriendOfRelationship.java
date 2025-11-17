package com.example.domain.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

/**
 * 好友关系
 */
@RelationshipProperties
@Data
public class FriendOfRelationship {
    
    @Id
    @GeneratedValue
    private Long id;
    
    private LocalDateTime createdAt;
    private String remark;  // 好友备注
    private String source;  // 添加来源：SEARCH, GROUP, QR_CODE
    
    @TargetNode
    private UserNode friend;
}
