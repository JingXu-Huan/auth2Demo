package com.example.relationship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.relationship.entity.FriendInteraction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendInteractionMapper extends BaseMapper<FriendInteraction> {
    
    @Select("SELECT * FROM friend_interactions WHERE user_id = #{userId} " +
            "ORDER BY interaction_score DESC LIMIT #{limit}")
    List<FriendInteraction> selectTopByScore(@Param("userId") Long userId, @Param("limit") int limit);
    
    @Select("SELECT * FROM friend_interactions WHERE user_id = #{userId} AND friend_id = #{friendId}")
    FriendInteraction selectByUserAndFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
