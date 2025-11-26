package com.example.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 会话/频道实体
 * 对应 im_db.channels 表
 */
@Data
@TableName("channels")
public class Channel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话类型：1-单聊, 2-群聊, 3-系统通知, 4-频道
     */
    private Integer channelType;

    /**
     * 群聊名称
     */
    private String name;

    /**
     * 群聊头像
     */
    private String avatarUrl;

    /**
     * 群主ID
     */
    private Long ownerId;

    /**
     * 成员数量
     */
    private Integer memberCount;

    /**
     * 最大成员数
     */
    private Integer maxMembers;

    /**
     * 群公告
     */
    private String announcement;

    /**
     * 群描述
     */
    private String description;

    /**
     * 群设置 (JSON)
     */
    private String settings;

    /**
     * 功能开关 (JSON)
     */
    private String features;

    /**
     * 加群类型：1-自由加入, 2-需审批, 3-禁止加入
     */
    private Integer joinType;

    /**
     * 入群问题
     */
    private String joinQuestion;

    /**
     * 状态：1-正常, 2-解散, 3-封禁
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    /**
     * 软删除时间
     */
    private OffsetDateTime deletedAt;
    
    /**
     * 对方用户名（私聊时使用，非数据库字段）
     */
    @TableField(exist = false)
    private String displayName;
    
    /**
     * 对方用户ID（私聊时使用，非数据库字段）
     */
    @TableField(exist = false)
    private Long targetUserId;

    /**
     * 判断是否为小群（写扩散阈值）
     */
    public boolean isSmallGroup() {
        return memberCount != null && memberCount < 500;
    }
}
