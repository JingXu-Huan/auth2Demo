package com.example.domain.dto;

import lombok.Data;

/**
 * 正在输入信令请求体
 *
 * <p>用于发送"正在输入"状态的请求参数。</p>
 */
@Data
public class TypingIndicatorRequest {

    /**
     * 发送输入状态的用户ID
     */
    private String fromUserId;

    /**
     * 接收输入状态的用户ID（单聊）
     */
    private String toUserId;

    /**
     * 群组ID（群聊，可选）
     */
    private String groupId;

    /**
     * 所属会话ID
     */
    private String conversationId;

    /**
     * 是否正在输入：true（开始输入）或 false（停止输入）
     */
    private Boolean typing;
}
