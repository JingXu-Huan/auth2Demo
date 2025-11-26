package com.example.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.im.entity.MessageInbox;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 消息收件箱 Mapper（写扩散）
 */
@Mapper
public interface MessageInboxMapper extends BaseMapper<MessageInbox> {

    /**
     * 批量插入收件箱
     */
    @Insert({
        "<script>",
        "INSERT INTO message_inbox (user_id, message_id, channel_id, sender_id, msg_type, preview, is_read, is_deleted, is_mentioned, created_at) VALUES ",
        "<foreach collection='list' item='item' separator=','>",
        "(#{item.userId}, #{item.messageId}, #{item.channelId}, #{item.senderId}, #{item.msgType}, #{item.preview}, #{item.isRead}, #{item.isDeleted}, #{item.isMentioned}, #{item.createdAt})",
        "</foreach>",
        "</script>"
    })
    int batchInsert(@Param("list") List<MessageInbox> inboxList);

    /**
     * 按用户ID查询未读消息
     */
    @Select("SELECT * FROM message_inbox WHERE user_id = #{userId} AND is_read = FALSE AND is_deleted = FALSE ORDER BY created_at DESC LIMIT #{limit}")
    List<MessageInbox> findUnreadByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 按用户ID和序号查询消息（用于同步）
     */
    @Select("SELECT * FROM message_inbox WHERE user_id = #{userId} AND message_id > #{cursor} AND is_deleted = FALSE ORDER BY message_id ASC LIMIT #{limit}")
    List<MessageInbox> findByUserIdAndMessageIdGreaterThan(@Param("userId") Long userId,
                                                           @Param("cursor") Long cursor,
                                                           @Param("limit") Integer limit);

    /**
     * 标记消息已读
     */
    @Update("UPDATE message_inbox SET is_read = TRUE WHERE user_id = #{userId} AND channel_id = #{channelId} AND is_read = FALSE")
    int markAsRead(@Param("userId") Long userId, @Param("channelId") Long channelId);

    /**
     * 统计未读消息数
     */
    @Select("SELECT COUNT(1) FROM message_inbox WHERE user_id = #{userId} AND channel_id = #{channelId} AND is_read = FALSE AND is_deleted = FALSE")
    int countUnread(@Param("userId") Long userId, @Param("channelId") Long channelId);
}
