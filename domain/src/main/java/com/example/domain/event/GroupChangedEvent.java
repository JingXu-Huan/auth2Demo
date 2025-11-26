package com.example.domain.event;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群组变更事件，对应表 im_group
 */
@Data
public class GroupChangedEvent {

    /** 群组主键 ID */
    private Long id;

    /** 业务唯一标识 */
    private String groupId;

    /** 群组名称 */
    private String name;

    /** 群组描述 */
    private String description;

    /** 群组头像 */
    private String avatar;

    /** 群主用户ID */
    private Long ownerId;

    /** 最大成员数 */
    private Integer maxMembers;

    /** 加群方式 */
    private String joinType;

    /** 群公告 */
    private String announcement;

    /** 状态：1=正常, 0=已解散 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 操作类型：UPSERT / DELETE */
    private String operation;
}
