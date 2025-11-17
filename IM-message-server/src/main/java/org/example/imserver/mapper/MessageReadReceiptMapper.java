package org.example.imserver.mapper;

import com.example.domain.model.MessageReadReceiptDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 消息已读回执记录Mapper
 */
@Repository
public interface MessageReadReceiptMapper extends MongoRepository<MessageReadReceiptDocument, String> {

    /**
     * 查询某条消息的所有已读回执
     */
    List<MessageReadReceiptDocument> findByMessageId(String messageId);

    /**
     * 统计某条消息的已读人数
     */
    long countByMessageId(String messageId);

    /**
     * 检查某用户是否已读某条消息
     */
    boolean existsByMessageIdAndUserId(String messageId, String userId);

    /**
     * 查询某用户在某会话中的所有已读回执
     */
    List<MessageReadReceiptDocument> findByUserIdAndConversationId(String userId, String conversationId);

    /**
     * 删除某条消息的所有已读回执（消息删除时清理）
     */
    void deleteByMessageId(String messageId);
}
