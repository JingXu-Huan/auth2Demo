package com.example.domain.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 用户收藏消息记录
 *
 * <p>每条记录表示某用户收藏了某条消息，
 * 用于实现消息收藏功能。</p>
 */
@Data
@Document(collection = "favorite_messages")
public class FavoriteMessageDocument {

    @Id
    private String id;

    /**
     * 收藏操作的用户ID
     */
    private String userId;

    /**
     * 被收藏的消息ID
     */
    private String messageId;

    /**
     * 所属会话ID
     */
    private String conversationId;

    /**
     * 收藏时间戳（毫秒）
     */
    private Long createdAt;
}
