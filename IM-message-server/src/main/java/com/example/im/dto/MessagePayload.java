package com.example.im.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 消息负载DTO（用于MQ传输）
 */
@Data
public class MessagePayload {

    /**
     * 消息ID（雪花算法）
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
     * 消息序号
     */
    private Long seqId;

    /**
     * 消息类型
     */
    private Integer msgType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 媒体URL列表
     */
    private List<String> mediaUrls;

    /**
     * 回复的消息ID
     */
    private Long replyToMsgId;

    /**
     * 回复的用户ID
     */
    private Long replyToUserId;

    /**
     * @提醒的用户ID列表
     */
    private List<Long> mentionedUserIds;

    /**
     * 是否@全体成员
     */
    private Boolean mentionAll;

    /**
     * 扩展信息
     */
    private String extra;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;

    /**
     * 是否采用写扩散（小群）
     */
    private boolean writeDiffusion;

    /**
     * 接收者ID列表（写扩散时使用）
     */
    private List<Long> receiverIds;
}
