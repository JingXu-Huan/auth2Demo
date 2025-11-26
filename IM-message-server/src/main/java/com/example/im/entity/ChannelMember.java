package com.example.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 会话成员实体
 * 对应 im_db.channel_members 表
 */
@Data
@TableName("channel_members")
public class ChannelMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private Long channelId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 成员角色：1-普通成员, 2-管理员, 3-群主
     */
    private Integer role;

    /**
     * 群昵称
     */
    private String nickname;

    /**
     * 最后已读消息ID
     */
    private Long lastReadMsgId;

    /**
     * 最后已读序号
     */
    private Long lastReadSeq;

    /**
     * 最后已读时间
     */
    private OffsetDateTime lastReadTime;

    /**
     * 未读消息数
     */
    private Integer unreadCount;

    /**
     * @消息数
     */
    private Integer mentionCount;

    /**
     * 免打扰截止时间
     */
    private OffsetDateTime mutedUntil;

    /**
     * 是否置顶
     */
    private Boolean pinned;

    /**
     * 是否显示群昵称
     */
    private Boolean showNickname;

    /**
     * 邀请人ID
     */
    private Long invitedBy;

    /**
     * 加入时间
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime joinedAt;

    /**
     * 退出时间
     */
    private OffsetDateTime leftAt;
}
