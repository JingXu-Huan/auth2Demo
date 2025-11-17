package org.example.imserver.mapper;

import com.example.domain.model.FavoriteMessageDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户收藏消息记录Mapper
 */
@Repository
public interface FavoriteMessageMapper extends MongoRepository<FavoriteMessageDocument, String> {

    /**
     * 分页查询某用户的收藏消息（按收藏时间倒序）
     */
    Page<FavoriteMessageDocument> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * 分页查询某用户在某会话中的收藏消息
     */
    Page<FavoriteMessageDocument> findByUserIdAndConversationIdOrderByCreatedAtDesc(String userId, String conversationId, Pageable pageable);

    /**
     * 检查某用户是否收藏了某条消息
     */
    boolean existsByUserIdAndMessageId(String userId, String messageId);

    /**
     * 删除某用户对某条消息的收藏
     */
    void deleteByUserIdAndMessageId(String userId, String messageId);

    /**
     * 获取某用户的所有收藏消息ID（用于批量查询消息详情）
     */
    List<FavoriteMessageDocument> findByUserId(String userId);
}
