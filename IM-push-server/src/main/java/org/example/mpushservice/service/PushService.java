package org.example.mpushservice.service;

import com.example.domain.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 推送服务
 * 负责处理从 MQ 接收到的消息，直接通过 WebSocket 推送给用户
 * 优化：不再依赖 Feign 远程调用，所有受众列表由业务服务提供
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final WebSocketPushService webSocketPushService;

    /**
     * 处理系统事件推送（携带受众列表）
     * 优化：直接使用消息中携带的受众列表，不再查询
     * @param event 系统事件消息
     * @param recipientIds 受众 ID 列表
     */
    public void pushSystemEventWithRecipients(ChatMessage event, List<String> recipientIds) {
        try {
            // 设置时间戳
            if (event.getCreatedAt() == null) {
                event.setCreatedAt(System.currentTimeMillis());
            }

            log.info("处理系统事件推送: conversationId={}, eventType={}, recipientCount={}", 
                event.getConversationId(),
                event.getPayload() != null ? ((Map<?, ?>)event.getPayload()).get("eventType") : "unknown",
                recipientIds != null ? recipientIds.size() : 0);

            // 直接推送到受众列表
            if (recipientIds != null && !recipientIds.isEmpty()) {
                webSocketPushService.pushSystemEvent(event, recipientIds);
                log.info("系统事件已推送到 {} 个用户", recipientIds.size());
            } else {
                log.warn("系统事件受众列表为空: conversationId={}", event.getConversationId());
            }

            log.info("系统事件推送完成: conversationId={}", event.getConversationId());
        } catch (Exception e) {
            log.error("推送系统事件失败: conversationId={}", event.getConversationId(), e);
            throw new RuntimeException("推送系统事件失败", e);
        }
    }


    /**
     * 处理聊天消息推送（携带受众列表）
     * 优化：直接使用消息中携带的受众列表，不再查询
     * @param message 聊天消息
     * @param recipientIds 受众 ID 列表
     */
    public void pushChatMessageWithRecipients(ChatMessage message, List<String> recipientIds) {
        try {
            int originalCount = recipientIds != null ? recipientIds.size() : 0;
            log.info("处理聊天消息推送: messageId={}, channelType={}, recipientCount={}", 
                message.getMessageId(), message.getChannelType(), originalCount);

            if (recipientIds == null || recipientIds.isEmpty()) {
                log.warn("聊天消息受众列表为空: messageId={}", message.getMessageId());
                return;
            }

            // 统一处理：过滤掉发送者自己（无论单聊还是群聊）
            List<String> targetIds = recipientIds.stream()
                    .filter(id -> id != null && !id.equals(message.getSenderId()))
                    .collect(Collectors.toList());

            if (targetIds.isEmpty()) {
                log.warn("聊天消息受众列表过滤后为空(全部为发送者自己): messageId={}", message.getMessageId());
                return;
            }

            webSocketPushService.pushMessageToUsers(message, targetIds);
            log.info("聊天消息已推送到 {} 个用户", targetIds.size());
        } catch (Exception e) {
            log.error("推送聊天消息失败: messageId={}", message.getMessageId(), e);
        }
    }

    /**
     * 处理聊天消息推送
     * @param message 聊天消息
     */
    public void pushChatMessage(ChatMessage message) {
        try {
            log.info("处理聊天消息推送: messageId={}, channelType={}", 
                message.getMessageId(), message.getChannelType());

            if (message.getChannelType() == ChatMessage.ChannelType.PRIVATE) {
                // 单聊消息
                webSocketPushService.pushPrivateMessage(message);
            } else if (message.getChannelType() == ChatMessage.ChannelType.GROUP) {
                // 群聊消息如果没有携带受众列表，不再在推送服务中查询
                log.warn("收到未携带受众列表的群聊消息, 请使用 ChatMessageWithRecipients 流程: messageId={}",
                    message.getMessageId());
            }
        } catch (Exception e) {
            log.error("推送聊天消息失败: messageId={}", message.getMessageId(), e);
        }
    }
}
