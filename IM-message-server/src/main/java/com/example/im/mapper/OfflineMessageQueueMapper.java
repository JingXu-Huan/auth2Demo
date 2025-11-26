package com.example.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.im.entity.OfflineMessageQueue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OfflineMessageQueueMapper extends BaseMapper<OfflineMessageQueue> {
    
    @Select("SELECT * FROM offline_message_queue WHERE user_id = #{userId} " +
            "AND delivered = FALSE ORDER BY priority DESC, created_at ASC LIMIT #{limit}")
    List<OfflineMessageQueue> selectPendingByUser(@Param("userId") Long userId, @Param("limit") int limit);
    
    @Update("UPDATE offline_message_queue SET delivered = TRUE, delivered_at = NOW() " +
            "WHERE id = #{id}")
    int markAsDelivered(@Param("id") Long id);
}
