package com.example.relationship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.relationship.entity.FriendRecommendation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendRecommendationMapper extends BaseMapper<FriendRecommendation> {
    
    @Select("SELECT * FROM friend_recommendations WHERE user_id = #{userId} " +
            "AND feedback IS NULL AND expires_at > NOW() ORDER BY score DESC LIMIT #{limit}")
    List<FriendRecommendation> selectActiveByUser(@Param("userId") Long userId, @Param("limit") int limit);
    
    @Delete("DELETE FROM friend_recommendations WHERE expires_at < NOW()")
    int deleteExpired();
}
