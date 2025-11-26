package com.example.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.im.entity.Channel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 会话/频道 Mapper
 */
@Mapper
public interface ChannelMapper extends BaseMapper<Channel> {

    /**
     * 获取会话成员数量
     */
    @Select("SELECT member_count FROM channels WHERE id = #{channelId}")
    Integer getMemberCount(@Param("channelId") Long channelId);

    /**
     * 更新成员数量
     */
    @Update("UPDATE channels SET member_count = member_count + #{delta} WHERE id = #{channelId}")
    int updateMemberCount(@Param("channelId") Long channelId, @Param("delta") Integer delta);

    /**
     * 判断是否为小群（成员数<500）
     */
    @Select("SELECT member_count < 500 FROM channels WHERE id = #{channelId}")
    Boolean isSmallGroup(@Param("channelId") Long channelId);
}
