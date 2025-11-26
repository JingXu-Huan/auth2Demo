package com.example.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.im.entity.MessageReaction;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageReactionMapper extends BaseMapper<MessageReaction> {

    /**
     * 获取消息的所有反应
     */
    @Select("SELECT * FROM message_reactions WHERE message_id = #{messageId} ORDER BY created_at")
    List<MessageReaction> findByMessage(@Param("messageId") Long messageId);

    /**
     * 删除反应
     */
    @Delete("DELETE FROM message_reactions WHERE message_id = #{messageId} AND user_id = #{userId} AND emoji = #{emoji}")
    int deleteReaction(@Param("messageId") Long messageId, @Param("userId") Long userId, @Param("emoji") String emoji);

    /**
     * 统计某个emoji的数量
     */
    @Select("SELECT COUNT(*) FROM message_reactions WHERE message_id = #{messageId} AND emoji = #{emoji}")
    int countByEmoji(@Param("messageId") Long messageId, @Param("emoji") String emoji);
}
