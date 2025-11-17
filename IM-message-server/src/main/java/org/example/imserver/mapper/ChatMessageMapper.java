package org.example.imserver.mapper;

import com.example.domain.dto.ChatMessage;
import com.example.domain.model.ChatMessageDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageMapper extends MongoRepository<ChatMessageDocument, String> {

    Page<ChatMessageDocument> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    Page<ChatMessageDocument> findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(String conversationId,
                                                                                            Long createdAt,
                                                                                            Pageable pageable);

    Optional<ChatMessageDocument> findByMessageId(String messageId);

    List<ChatMessageDocument> findByMessageIdIn(Iterable<String> messageIds);

    List<ChatMessageDocument> findByReceiverIdAndStatusNot(String receiverId, ChatMessage.MessageStatus status);

    /**
     * 查询某用户作为发送者的所有消息
     */
    List<ChatMessageDocument> findBySenderId(String senderId);

    /**
     * 查询某用户作为接收者的所有消息
     */
    List<ChatMessageDocument> findByReceiverId(String receiverId);
}
