package com.example.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 消息收件箱实体（写扩散模式）
 * 对应 im_db.message_inbox 分区表
 * 用于500人以下群组的消息投递
 */
@Data
@TableName("message_inbox")
public class MessageInbox {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收者用户ID（分区键）
     */
    private Long userId;

    /**
     * 消息ID
     */
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
     * 消息类型
     */
    private Integer msgType;

    /**
     * 消息预览（前100字符）
     */
    private String preview;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 是否已删除
     */
    private Boolean isDeleted;

    /**
     * 是否被@
     */
    private Boolean isMentioned;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
