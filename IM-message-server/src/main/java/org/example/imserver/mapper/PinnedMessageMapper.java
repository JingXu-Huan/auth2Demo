package org.example.imserver.mapper;

import com.example.domain.model.PinnedMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户置顶消息记录Mapper
 */
@Repository
public interface PinnedMessageMapper extends MongoRepository<PinnedMessageDocument, String> {

    /**
     * 查询某用户在某会话中的所有置顶消息（按置顶时间倒序）
     */
    List<PinnedMessageDocument> findByUserIdAndConversationIdOrderByPinnedAtDesc(String userId, String conversationId);

    /**
     * 查询某用户的所有置顶消息
     */
    List<PinnedMessageDocument> findByUserIdOrderByPinnedAtDesc(String userId);

    /**
     * 检查某用户是否置顶了某条消息
     */
    boolean existsByUserIdAndConversationIdAndMessageId(String userId, String conversationId, String messageId);

    /**
     * 删除某用户在某会话中对某条消息的置顶
     */
    void deleteByUserIdAndConversationIdAndMessageId(String userId, String conversationId, String messageId);
}
