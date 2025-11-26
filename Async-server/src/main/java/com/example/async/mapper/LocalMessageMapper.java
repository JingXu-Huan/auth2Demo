package com.example.async.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.async.entity.LocalMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息 Mapper
 */
@Mapper
public interface LocalMessageMapper extends BaseMapper<LocalMessage> {

    /**
     * 查询待发送或需要重试的消息
     */
    @Select("SELECT * FROM local_messages WHERE status IN (0, 3) " +
            "AND (next_retry_at IS NULL OR next_retry_at <= #{now}) " +
            "AND retry_count < max_retry " +
            "ORDER BY created_at LIMIT #{limit}")
    List<LocalMessage> findPendingMessages(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * 更新消息状态为已发送
     */
    @Update("UPDATE local_messages SET status = 1, sent_at = #{sentAt} WHERE id = #{id}")
    int markAsSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);

    /**
     * 更新消息状态为已确认
     */
    @Update("UPDATE local_messages SET status = 2, confirmed_at = #{confirmedAt} WHERE message_id = #{messageId}")
    int markAsConfirmed(@Param("messageId") String messageId, @Param("confirmedAt") LocalDateTime confirmedAt);

    /**
     * 更新消息状态为失败
     */
    @Update("UPDATE local_messages SET status = 3, retry_count = retry_count + 1, " +
            "next_retry_at = #{nextRetryAt}, error_code = #{errorCode}, error_message = #{errorMessage}, " +
            "failed_at = #{failedAt} WHERE id = #{id}")
    int markAsFailed(@Param("id") Long id, @Param("nextRetryAt") LocalDateTime nextRetryAt,
                     @Param("errorCode") String errorCode, @Param("errorMessage") String errorMessage,
                     @Param("failedAt") LocalDateTime failedAt);
}
