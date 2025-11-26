package com.example.relationship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.relationship.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 好友申请Mapper
 */
@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {
    
    /**
     * 查询待处理的申请（收到的）
     */
    @Select("SELECT * FROM friend_requests WHERE receiver_id = #{userId} AND status = 0 AND expires_at > NOW() ORDER BY created_at DESC")
    List<FriendRequest> findPendingRequests(@Param("userId") Long userId);
    
    /**
     * 查询已发送的申请
     */
    @Select("SELECT * FROM friend_requests WHERE sender_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<FriendRequest> findSentRequests(@Param("userId") Long userId, @Param("limit") int limit);
    
    /**
     * 查询两人之间是否有待处理的申请
     */
    @Select("SELECT * FROM friend_requests WHERE sender_id = #{senderId} AND receiver_id = #{receiverId} AND status = 0 LIMIT 1")
    FriendRequest findPendingBetween(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
    
    /**
     * 统计待处理申请数量
     */
    @Select("SELECT COUNT(*) FROM friend_requests WHERE receiver_id = #{userId} AND status = 0 AND expires_at > NOW()")
    int countPending(@Param("userId") Long userId);
}
