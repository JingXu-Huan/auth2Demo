package com.example.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 消息删除请求体
 *
 * <p>用于用户本地删除消息的请求参数。</p>
 */
@Data
public class DeleteMessageRequest {

    /**
     * 执行删除操作的用户ID
     */
    private String userId;

    /**
     * 要删除的消息ID列表
     */
    private List<String> messageIds;
}
