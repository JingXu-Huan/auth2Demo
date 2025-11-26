package com.example.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.im.entity.MessageRead;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageReadMapper extends BaseMapper<MessageRead> {
    
    @Select("SELECT COUNT(*) FROM message_reads WHERE message_id = #{messageId}")
    int countByMessageId(@Param("messageId") Long messageId);
    
    @Select("SELECT user_id FROM message_reads WHERE message_id = #{messageId}")
    List<Long> selectReadUserIds(@Param("messageId") Long messageId);

    /**
     * 标记消息已读
     */
    @Insert("INSERT INTO message_reads (message_id, channel_id, user_id, read_at) " +
            "VALUES (#{messageId}, #{channelId}, #{userId}, NOW()) " +
            "ON CONFLICT (message_id, user_id) DO NOTHING")
    int markAsRead(@Param("messageId") Long messageId, @Param("channelId") Long channelId, @Param("userId") Long userId);

    /**
     * 统计已读人数
     */
    @Select("SELECT COUNT(*) FROM message_reads WHERE message_id = #{messageId}")
    int countReads(@Param("messageId") Long messageId);

    /**
     * 获取已读用户ID列表
     */
    @Select("SELECT user_id FROM message_reads WHERE message_id = #{messageId} ORDER BY read_at DESC LIMIT #{limit}")
    List<Long> getReaderIds(@Param("messageId") Long messageId, @Param("limit") int limit);
}
