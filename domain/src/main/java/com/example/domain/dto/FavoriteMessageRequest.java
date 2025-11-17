package com.example.domain.dto;

import lombok.Data;

/**
 * 消息收藏请求体
 *
 * <p>用于添加或移除消息收藏的请求参数。</p>
 */
@Data
public class FavoriteMessageRequest {

    /**
     * 执行收藏操作的用户ID
     */
    private String userId;

    /**
     * 要收藏的消息ID
     */
    private String messageId;

    /**
     * 操作类型：ADD（添加收藏）或 REMOVE（移除收藏）
     */
    private String action;
}
