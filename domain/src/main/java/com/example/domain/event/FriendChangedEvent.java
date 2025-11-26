package com.example.domain.event;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友关系变更事件，对应表 im_friend
 */
@Data
public class FriendChangedEvent {

    /** 关联主键 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 好友 ID */
    private Long friendId;

    /** 好友备注 */
    private String remark;

    /** 添加来源 */
    private String source;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 操作类型：UPSERT / DELETE */
    private String operation;
}
