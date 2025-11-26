package com.example.domain.event;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友请求变更事件，对应表 im_friend_request
 */
@Data
public class FriendRequestChangedEvent {

    /** 请求主键 ID */
    private Long id;

    /** 发起用户 ID */
    private Long fromUserId;

    /** 目标用户 ID */
    private Long toUserId;

    /** 请求消息 */
    private String message;

    /** 状态：PENDING/ACCEPTED/REJECTED */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 操作类型：UPSERT / DELETE */
    private String operation;
}
