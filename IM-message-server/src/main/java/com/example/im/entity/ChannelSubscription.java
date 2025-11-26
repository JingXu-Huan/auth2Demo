package com.example.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 频道订阅实体
 * 对应 im_db.channel_subscriptions 表
 */
@Data
@Accessors(chain = true)
@TableName("channel_subscriptions")
public class ChannelSubscription {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 频道ID */
    private Long channelId;

    /** 用户ID */
    private Long userId;

    /** 是否接收推送 */
    private Boolean receivePush;

    /** 订阅时间 */
    private LocalDateTime subscribedAt;

    /** 取消订阅时间 */
    private LocalDateTime unsubscribedAt;
}
