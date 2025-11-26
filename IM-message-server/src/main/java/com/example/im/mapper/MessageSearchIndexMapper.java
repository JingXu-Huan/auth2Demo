package com.example.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.im.entity.MessageSearchIndex;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息搜索索引Mapper
 */
@Mapper
public interface MessageSearchIndexMapper extends BaseMapper<MessageSearchIndex> {

    @Select("SELECT message_id FROM message_search_index WHERE channel_id = #{channelId} " +
            "AND content_tsv @@ plainto_tsquery(#{keyword}) ORDER BY created_at DESC LIMIT #{limit}")
    List<Long> searchInChannel(@Param("channelId") Long channelId, 
                                @Param("keyword") String keyword, 
                                @Param("limit") int limit);

    @Select("SELECT message_id FROM message_search_index WHERE sender_id = #{userId} " +
            "AND content_tsv @@ plainto_tsquery(#{keyword}) ORDER BY created_at DESC LIMIT #{limit}")
    List<Long> searchByUser(@Param("userId") Long userId, 
                            @Param("keyword") String keyword, 
                            @Param("limit") int limit);
}
