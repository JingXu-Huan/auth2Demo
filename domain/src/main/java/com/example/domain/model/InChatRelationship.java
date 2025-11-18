package com.example.domain.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

/**
 * 用户参与会话关系
 */
@Data
@RelationshipProperties
public class InChatRelationship {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * 参与角色（预留：OWNER / MEMBER 等），当前可选
     */
    private String role;

    /**
     * 加入会话时间
     */
    private LocalDateTime joinedAt;

    @TargetNode
    private ConversationNode conversation;
}
