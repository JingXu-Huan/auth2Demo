package com.example.im.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 发送消息请求DTO
 */
@Data
public class SendMessageRequest {

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private Long channelId;

    /**
     * 消息类型：1-文本, 2-图片, 3-文件, 4-语音, 5-视频
     */
    @NotNull(message = "消息类型不能为空")
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
     * @提醒的用户ID列表
     */
    private List<Long> mentionedUserIds;

    /**
     * 是否@全体成员
     */
    private Boolean mentionAll;

    /**
     * 扩展信息 (JSON)
     */
    private String extra;

    /**
     * 转发的原消息ID
     */
    private Long forwardFromMsgId;

    /**
     * 转发的原频道ID
     */
    private Long forwardFromChannelId;
}
