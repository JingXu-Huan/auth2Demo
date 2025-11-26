package com.example.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.im.entity.ChannelSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 频道订阅Mapper
 */
@Mapper
public interface ChannelSubscriptionMapper extends BaseMapper<ChannelSubscription> {

    @Select("SELECT * FROM channel_subscriptions WHERE channel_id = #{channelId} AND unsubscribed_at IS NULL")
    List<ChannelSubscription> findByChannel(@Param("channelId") Long channelId);

    @Select("SELECT * FROM channel_subscriptions WHERE user_id = #{userId} AND unsubscribed_at IS NULL")
    List<ChannelSubscription> findByUser(@Param("userId") Long userId);

    @Select("SELECT user_id FROM channel_subscriptions WHERE channel_id = #{channelId} AND receive_push = true AND unsubscribed_at IS NULL")
    List<Long> findPushUserIds(@Param("channelId") Long channelId);

    @Update("UPDATE channel_subscriptions SET unsubscribed_at = NOW() WHERE channel_id = #{channelId} AND user_id = #{userId}")
    int unsubscribe(@Param("channelId") Long channelId, @Param("userId") Long userId);
}
