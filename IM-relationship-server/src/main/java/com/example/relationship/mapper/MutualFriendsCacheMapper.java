package com.example.relationship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.relationship.entity.MutualFriendsCache;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MutualFriendsCacheMapper extends BaseMapper<MutualFriendsCache> {
    
    @Select("SELECT * FROM mutual_friends_cache WHERE " +
            "(user1_id = #{user1Id} AND user2_id = #{user2Id}) OR " +
            "(user1_id = #{user2Id} AND user2_id = #{user1Id})")
    MutualFriendsCache selectByUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
    
    @Delete("DELETE FROM mutual_friends_cache WHERE expires_at < NOW()")
    int deleteExpired();
}
