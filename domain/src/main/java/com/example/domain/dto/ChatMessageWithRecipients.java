package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 携带受众列表的聊天消息
 * 用于在 MQ 中传输消息时，同时携带需要推送的用户 ID 列表
 * 遵循"业务与通讯分离"原则：业务服务负责查询受众，推送服务只负责发送
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageWithRecipients implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 聊天消息内容
     */
    private ChatMessage message;
    
    /**
     * 接收者 ID 列表
     * - 群聊：所有群成员的 userId
     * - 单聊：接收者的 userId
     * - 系统事件：需要通知的所有用户 userId
     */
    private List<String> recipientIds;
}
