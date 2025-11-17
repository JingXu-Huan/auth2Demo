package com.example.domain.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 消息已读回执记录
 *
 * <p>每条记录表示某用户已读了某条消息，
 * 主要用于群聊中统计已读人数。</p>
 */
@Data
@Document(collection = "message_read_receipts")
public class MessageReadReceiptDocument {

    @Id
    private String id;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 已读用户ID
     */
    private String userId;

    /**
     * 所属会话ID
     */
    private String conversationId;

    /**
     * 群组ID（可选，仅群聊消息有值）
     */
    private String groupId;

    /**
     * 已读时间戳（毫秒）
     */
    private Long readAt;
}
