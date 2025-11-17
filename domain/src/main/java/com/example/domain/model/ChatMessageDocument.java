package com.example.domain.model;

import com.example.domain.dto.ChatMessage;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "chat_messages")
public class ChatMessageDocument {

    @Id
    private String id;

    private String messageId;

    private String clientMsgId;

    private String conversationId;

    private Long seq;

    private ChatMessage.MessageStatus status;

    private Long createdAt;

    private String senderId;

    private String receiverId;

    private String groupId;

    private ChatMessage.ChannelType channelType;

    private ChatMessage.ContentType contentType;

    private Object payload;

    /**
     * 撤回时间戳（毫秒），仅在状态为 RECALLED 时有值
     */
    private Long recalledAt;

    /**
     * 被回复的消息ID（用于消息引用）
     */
    private String replyToMessageId;
}
