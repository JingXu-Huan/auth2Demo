package com.example.domain.dto;

import lombok.Data;

/**
 * 消息置顶请求体
 *
 * <p>用于置顶或取消置顶消息的请求参数。</p>
 */
@Data
public class PinMessageRequest {

    /**
     * 执行置顶操作的用户ID
     */
    private String userId;

    /**
     * 所属会话ID
     */
    private String conversationId;

    /**
     * 要置顶的消息ID
     */
    private String messageId;

    /**
     * 操作类型：PIN（置顶）或 UNPIN（取消置顶）
     */
    private String action;
}
