package com.example.relationship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.relationship.entity.FriendRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 好友关系Mapper
 */
@Mapper
public interface FriendRelationMapper extends BaseMapper<FriendRelation> {
    
    /**
     * 查询用户的好友列表
     */
    @Select("SELECT * FROM friend_relations WHERE user_id = #{userId} AND deleted_at IS NULL ORDER BY starred DESC, remark, created_at DESC")
    List<FriendRelation> findFriends(@Param("userId") Long userId);
    
    /**
     * 查询两人是否是好友
     */
    @Select("SELECT * FROM friend_relations WHERE user_id = #{userId} AND friend_id = #{friendId} AND deleted_at IS NULL LIMIT 1")
    FriendRelation findRelation(@Param("userId") Long userId, @Param("friendId") Long friendId);
    
    /**
     * 查询星标好友
     */
    @Select("SELECT * FROM friend_relations WHERE user_id = #{userId} AND starred = true AND deleted_at IS NULL ORDER BY remark")
    List<FriendRelation> findStarredFriends(@Param("userId") Long userId);
    
    /**
     * 查询分组内好友
     */
    @Select("SELECT * FROM friend_relations WHERE user_id = #{userId} AND group_id = #{groupId} AND deleted_at IS NULL ORDER BY remark")
    List<FriendRelation> findByGroup(@Param("userId") Long userId, @Param("groupId") Long groupId);
    
    /**
     * 统计好友数量
     */
    @Select("SELECT COUNT(*) FROM friend_relations WHERE user_id = #{userId} AND deleted_at IS NULL")
    int countFriends(@Param("userId") Long userId);
    
    /**
     * 查询好友ID列表
     */
    @Select("SELECT friend_id FROM friend_relations WHERE user_id = #{userId} AND deleted_at IS NULL")
    List<Long> findFriendIds(@Param("userId") Long userId);
}
