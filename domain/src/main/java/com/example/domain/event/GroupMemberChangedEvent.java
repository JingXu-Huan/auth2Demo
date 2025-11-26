package com.example.domain.event;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群成员变更事件，对应表 im_group_member
 */
@Data
public class GroupMemberChangedEvent {

    /** 关联主键 ID */
    private Long id;

    /** 群组数据库 ID（im_group.id） */
    private Long groupId;

    /** 群组业务 ID（Group.groupId，用于 Neo4j） */
    private String groupIdStr;

    /** 用户 ID */
    private Long userId;

    /** 角色：OWNER/ADMIN/MEMBER */
    private String role;

    /** 群昵称 */
    private String groupNickname;

    /** 是否禁言 */
    private Boolean muted;

    /** 加入时间 */
    private LocalDateTime joinedAt;

    /** 操作类型：UPSERT / DELETE */
    private String operation;
}
