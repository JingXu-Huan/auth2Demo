package com.example.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.im.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息 Mapper（Timeline）
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 按会话和序号查询消息（用于同步）
     */
    @Select("SELECT * FROM messages WHERE channel_id = #{channelId} AND seq_id > #{cursor} ORDER BY seq_id ASC LIMIT #{limit}")
    List<Message> findByChannelIdAndSeqGreaterThan(@Param("channelId") Long channelId,
                                                    @Param("cursor") Long cursor,
                                                    @Param("limit") Integer limit);

    /**
     * 查询消息是否存在（用于事务回查）
     */
    @Select("SELECT COUNT(1) FROM messages WHERE message_id = #{messageId}")
    int countByMessageId(@Param("messageId") Long messageId);

    /**
     * 查询会话最新消息
     */
    @Select("SELECT * FROM messages WHERE channel_id = #{channelId} ORDER BY seq_id DESC LIMIT 1")
    Message findLatestByChannelId(@Param("channelId") Long channelId);
}
