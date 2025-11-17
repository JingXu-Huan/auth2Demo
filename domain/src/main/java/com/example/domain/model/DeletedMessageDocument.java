package com.example.domain.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 用户本地删除消息记录
 *
 * <p>每条记录表示某用户在某会话中"本地删除"了某条消息，
 * 该用户在查看历史时不会看到这条消息。</p>
 */
@Data
@Document(collection = "deleted_messages")
public class DeletedMessageDocument {

    @Id
    private String id;

    /**
     * 执行删除操作的用户ID
     */
    private String userId;

    /**
     * 被删除的消息ID
     */
    private String messageId;

    /**
     * 所属会话ID
     */
    private String conversationId;

    /**
     * 删除时间戳（毫秒）
     */
    private Long deletedAt;
}
