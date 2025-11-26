package com.example.relationship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.relationship.entity.Blacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 黑名单Mapper
 */
@Mapper
public interface BlacklistMapper extends BaseMapper<Blacklist> {
    
    /**
     * 查询用户的黑名单
     */
    @Select("SELECT * FROM blacklist WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Blacklist> findByUser(@Param("userId") Long userId);
    
    /**
     * 检查是否在黑名单中
     */
    @Select("SELECT * FROM blacklist WHERE user_id = #{userId} AND blocked_user_id = #{blockedUserId} LIMIT 1")
    Blacklist findBlocked(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);
    
    /**
     * 检查是否被对方拉黑
     */
    @Select("SELECT * FROM blacklist WHERE user_id = #{blockedUserId} AND blocked_user_id = #{userId} LIMIT 1")
    Blacklist findBlockedBy(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);
}
