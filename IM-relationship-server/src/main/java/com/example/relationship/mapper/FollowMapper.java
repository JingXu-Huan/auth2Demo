package com.example.relationship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.relationship.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
    
    @Select("SELECT following_id FROM follows WHERE follower_id = #{userId}")
    List<Long> selectFollowingIds(@Param("userId") Long userId);
    
    @Select("SELECT follower_id FROM follows WHERE following_id = #{userId}")
    List<Long> selectFollowerIds(@Param("userId") Long userId);
    
    @Select("SELECT COUNT(*) FROM follows WHERE follower_id = #{userId}")
    int countFollowing(@Param("userId") Long userId);
    
    @Select("SELECT COUNT(*) FROM follows WHERE following_id = #{userId}")
    int countFollowers(@Param("userId") Long userId);
}
