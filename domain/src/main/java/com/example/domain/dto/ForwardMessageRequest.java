package com.example.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 消息转发请求体
 *
 * <p>用于描述基于已有消息进行转发时的目标信息，
 * 包括被转发的消息ID、转发发起人以及目标用户/群组列表。</p>
 */
@Data
public class ForwardMessageRequest {

    /**
     * 原始消息ID（必须存在于消息存储中）
     */
    private String messageId;

    /**
     * 发起转发的用户ID（作为新消息的发送者），
     * 如果未传，则默认使用原消息的 senderId。
     */
    private String forwarderId;

    /**
     * 要转发到的单聊用户ID列表
     */
    private List<String> targetUserIds;

    /**
     * 要转发到的群组ID列表
     */
    private List<String> targetGroupIds;
}
