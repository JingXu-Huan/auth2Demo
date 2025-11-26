package com.example.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 消息实体 (Timeline)
 * 对应 im_db.messages 分区表
 */
@Data
@TableName("messages")
public class Message {

    /**
     * 消息ID（雪花算法）
     */
    @TableId(type = IdType.INPUT)
    private Long messageId;

    /**
     * 会话ID
     */
    private Long channelId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 消息序号（每个channel独立递增）
     */
    private Long seqId;

    /**
     * 消息类型：1-文本, 2-图片, 3-文件, 4-语音, 5-视频, 6-位置, 7-名片, 8-红包
     */
    private Integer msgType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 媒体URL数组 (JSON)
     */
    private String mediaUrls;

    /**
     * 媒体详情 (JSON)
     */
    private String mediaInfo;

    /**
     * 回复的消息ID
     */
    private Long replyToMsgId;

    /**
     * 回复的用户ID
     */
    private Long replyToUserId;

    /**
     * 转发来源消息ID
     */
    private Long forwardFromMsgId;

    /**
     * 转发来源会话ID
     */
    private Long forwardFromChannelId;

    /**
     * @提醒的用户ID列表 (JSON)
     */
    private String mentionedUserIds;

    /**
     * 是否@全体成员
     */
    private Boolean mentionAll;

    /**
     * 状态：1-正常, 2-撤回, 3-删除, 4-审核中
     */
    private Integer status;

    /**
     * 是否已编辑
     */
    private Boolean edited;

    /**
     * 编辑时间
     */
    private OffsetDateTime editedAt;

    /**
     * 扩展信息 (JSON)
     */
    private String extra;

    /**
     * 创建时间（分区键）
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
