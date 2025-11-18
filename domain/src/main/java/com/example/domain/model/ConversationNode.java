package com.example.domain.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

/**
 * 会话节点（统一抽象：单聊 / 群聊）
 */
@Data
@Node("Conversation")
public class ConversationNode {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * 业务会话ID
     * 单聊："minUserId-maxUserId"（如 1-2）
     * 群聊："GROUP:" + groupId（如 GROUP:group_xxx）
     */
    private String conversationId;

    /**
     * 会话类型：p2p / group
     */
    private String type;

    /**
     * 用于展示的会话名称（群聊：群名；单聊：可以为空，由上层根据对端用户动态生成）
     */
    private String name;

    /**
     * 会话头像（群聊：群头像；单聊：可留空）
     */
    private String avatar;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
