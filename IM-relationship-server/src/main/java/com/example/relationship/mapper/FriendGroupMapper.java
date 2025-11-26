package com.example.relationship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.relationship.entity.FriendGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 好友分组Mapper
 */
@Mapper
public interface FriendGroupMapper extends BaseMapper<FriendGroup> {
    
    /**
     * 查询用户的所有分组
     */
    @Select("SELECT * FROM friend_groups WHERE user_id = #{userId} ORDER BY sort_order, created_at")
    List<FriendGroup> findByUser(@Param("userId") Long userId);
    
    /**
     * 查询默认分组
     */
    @Select("SELECT * FROM friend_groups WHERE user_id = #{userId} AND is_default = true LIMIT 1")
    FriendGroup findDefault(@Param("userId") Long userId);
}
