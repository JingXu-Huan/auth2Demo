package com.example.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.im.entity.ChannelMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 会话成员 Mapper
 */
@Mapper
public interface ChannelMemberMapper extends BaseMapper<ChannelMember> {

    /**
     * 获取会话所有成员ID
     */
    @Select("SELECT user_id FROM channel_members WHERE channel_id = #{channelId} AND left_at IS NULL")
    List<Long> getMemberIds(@Param("channelId") Long channelId);

    /**
     * 检查用户是否在会话中
     */
    @Select("SELECT COUNT(1) > 0 FROM channel_members WHERE channel_id = #{channelId} AND user_id = #{userId} AND left_at IS NULL")
    boolean isMember(@Param("channelId") Long channelId, @Param("userId") Long userId);

    /**
     * 获取用户在会话中的角色
     */
    @Select("SELECT role FROM channel_members WHERE channel_id = #{channelId} AND user_id = #{userId} AND left_at IS NULL")
    Integer getMemberRole(@Param("channelId") Long channelId, @Param("userId") Long userId);

    /**
     * 更新最后已读序号
     */
    @Update("UPDATE channel_members SET last_read_seq = #{seqId}, last_read_time = NOW(), unread_count = 0 WHERE channel_id = #{channelId} AND user_id = #{userId}")
    int updateLastReadSeq(@Param("channelId") Long channelId, @Param("userId") Long userId, @Param("seqId") Long seqId);

    /**
     * 增加未读数
     */
    @Update("UPDATE channel_members SET unread_count = unread_count + 1 WHERE channel_id = #{channelId} AND user_id = #{userId} AND left_at IS NULL")
    int incrementUnreadCount(@Param("channelId") Long channelId, @Param("userId") Long userId);

    /**
     * 批量增加未读数
     */
    @Update("<script>" +
            "UPDATE channel_members SET unread_count = unread_count + 1 " +
            "WHERE channel_id = #{channelId} AND left_at IS NULL " +
            "AND user_id IN <foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>" +
            "</script>")
    int batchIncrementUnreadCount(@Param("channelId") Long channelId, @Param("userIds") List<Long> userIds);
    
    /**
     * 获取用户加入的频道ID列表
     */
    @Select("SELECT channel_id FROM channel_members WHERE user_id = #{userId} AND left_at IS NULL")
    List<Long> getChannelIdsByUser(@Param("userId") Long userId);
    
    /**
     * 离开频道(软删除)
     */
    @Update("UPDATE channel_members SET left_at = NOW() WHERE channel_id = #{channelId} AND user_id = #{userId}")
    int leaveChannel(@Param("channelId") Long channelId, @Param("userId") Long userId);
    
    /**
     * 获取频道成员列表
     */
    @Select("SELECT * FROM channel_members WHERE channel_id = #{channelId} AND left_at IS NULL ORDER BY joined_at")
    List<ChannelMember> getChannelMembers(@Param("channelId") Long channelId);
    
    /**
     * 获取成员信息（仅活跃成员）
     */
    @Select("SELECT * FROM channel_members WHERE channel_id = #{channelId} AND user_id = #{userId} AND left_at IS NULL")
    ChannelMember getMember(@Param("channelId") Long channelId, @Param("userId") Long userId);
    
    /**
     * 获取成员信息（包括已离开的）
     */
    @Select("SELECT * FROM channel_members WHERE channel_id = #{channelId} AND user_id = #{userId}")
    ChannelMember findMemberIncludeLeft(@Param("channelId") Long channelId, @Param("userId") Long userId);
    
    /**
     * 查找两个用户之间的私聊频道ID
     */
    @Select("SELECT cm1.channel_id FROM channel_members cm1 " +
            "JOIN channel_members cm2 ON cm1.channel_id = cm2.channel_id " +
            "JOIN channels c ON c.id = cm1.channel_id " +
            "WHERE cm1.user_id = #{userId1} AND cm2.user_id = #{userId2} " +
            "AND c.channel_type = 1 AND c.status = 1 " +
            "AND cm1.left_at IS NULL AND cm2.left_at IS NULL LIMIT 1")
    Long findPrivateChannelId(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
