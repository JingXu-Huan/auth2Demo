package org.example.mpushservice.service;

import com.example.domain.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mpushservice.handler.ChatWebSocketHandler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WebSocket 推送服务
 * 负责将消息推送到 WebSocket 客户端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPushService {

    private final ChatWebSocketHandler webSocketHandler;

    /**
     * 推送单聊消息
     */
    public void pushPrivateMessage(ChatMessage message) {
        String receiverId = message.getReceiverId();
        if (receiverId != null) {
            boolean sent = webSocketHandler.sendMessageToUser(receiverId, message);
            if (sent) {
                log.info("单聊消息已推送: receiverId={}, messageId={}", receiverId, message.getMessageId());
            } else {
                log.warn("单聊消息推送失败，用户不在线: receiverId={}", receiverId);
            }
        }
    }

    /**
     * 推送群聊消息
     */
    public void pushGroupMessage(ChatMessage message, List<String> memberIds) {
        // 排除发送者自己（发送者已经在前端显示了）
        List<String> targetIds = memberIds.stream()
                .filter(id -> !id.equals(message.getSenderId()))
                .collect(Collectors.toList());

        log.info("推送群聊消息: groupId={}, 目标用户数={}", message.getGroupId(), targetIds.size());
        
        webSocketHandler.sendMessageToUsers(targetIds, message);
    }

    /**
     * 通用：将消息推送到指定用户列表
     */
    public void pushMessageToUsers(ChatMessage message, List<String> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return;
        }
        webSocketHandler.sendMessageToUsers(targetUserIds, message);
    }

    /**
     * 推送系统事件
     */
    public void pushSystemEvent(ChatMessage event, List<String> targetUserIds) {
        log.info("推送系统事件: eventType={}, 目标用户数={}", 
            event.getPayload() != null ? ((java.util.Map<?, ?>)event.getPayload()).get("eventType") : "unknown",
            targetUserIds != null ? targetUserIds.size() : 0);

        if (targetUserIds != null && !targetUserIds.isEmpty()) {
            webSocketHandler.sendMessageToUsers(targetUserIds, event);
        } else {
            log.warn("系统事件没有目标用户");
        }
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String userId) {
        return webSocketHandler.isUserOnline(userId);
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineUserCount() {
        return webSocketHandler.getOnlineUserCount();
    }
}
