package org.example.imserver.mapper;

import com.example.domain.model.DeletedMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户删除消息记录Mapper
 */
@Repository
public interface DeletedMessageMapper extends MongoRepository<DeletedMessageDocument, String> {

    /**
     * 查询某用户在某会话中删除的所有消息ID
     */
    List<DeletedMessageDocument> findByUserIdAndConversationId(String userId, String conversationId);

    /**
     * 查询某用户删除的所有消息ID（跨会话）
     */
    List<DeletedMessageDocument> findByUserId(String userId);

    /**
     * 检查某用户是否删除了某条消息
     */
    boolean existsByUserIdAndMessageId(String userId, String messageId);
}
