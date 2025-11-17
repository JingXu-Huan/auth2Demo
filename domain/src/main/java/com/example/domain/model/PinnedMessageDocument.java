package com.example.domain.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 用户置顶消息记录
 *
 * <p>每条记录表示某用户在某会话中置顶了某条消息，
 * 用于实现消息置顶功能。</p>
 */
@Data
@Document(collection = "pinned_messages")
public class PinnedMessageDocument {

    @Id
    private String id;

    /**
     * 置顶操作的用户ID
     */
    private String userId;

    /**
     * 所属会话ID
     */
    private String conversationId;

    /**
     * 被置顶的消息ID
     */
    private String messageId;

    /**
     * 置顶时间戳（毫秒）
     */
    private Long pinnedAt;
}
