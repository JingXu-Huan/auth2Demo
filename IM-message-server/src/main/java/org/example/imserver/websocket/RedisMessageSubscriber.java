package org.example.imserver.websocket;

import com.example.domain.dto.ChatMessage;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class RedisMessageSubscriber {
    private final Gson gson = new Gson();
    private final WebSocketSessionManager sessionManager;

    public RedisMessageSubscriber(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void handleMessage(String messageJson) {
        log.info("Redis订阅收到消息:{}", messageJson);
        try {
            ChatMessage chatMessage = gson.fromJson(messageJson, ChatMessage.class);
            // 群聊消息：广播给所有在线用户（排除发送者），由前端根据 conversationId 过滤展示
            if (chatMessage.getChannelType() == ChatMessage.ChannelType.GROUP) {
                String groupId = chatMessage.getGroupId();
                String senderId = chatMessage.getSenderId();
                log.info("处理群聊消息推送, groupId={}, senderId={}", groupId, senderId);

                for (String onlineUserId : sessionManager.getOnlineUsers()) {
                    // 不推送给发送者本人，避免前端重复显示
                    if (onlineUserId != null && onlineUserId.equals(senderId)) {
                        continue;
                    }
                    try {
                        sessionManager.sendMessageToUser(onlineUserId, messageJson);
                    } catch (Exception e) {
                        log.error("推送群聊消息失败: onlineUserId={}, error={}", onlineUserId, e.getMessage());
                    }
                }
                return;
            }

            // 单聊消息：按 receiverId 精确投递
            String receiverId = chatMessage.getReceiverId();

            log.info("准备推送单聊消息给用户: receiverId={}, senderId={}", receiverId, chatMessage.getSenderId());
            log.info("当前在线用户列表: {}", sessionManager.getOnlineUsers());

            if (receiverId != null) {
                if (sessionManager.getOnlineUsers().contains(receiverId)) {
                    try {
                        sessionManager.sendMessageToUser(receiverId, messageJson);
                        log.info("消息成功推送给用户: {}", receiverId);
                    } catch (Exception e) {
                        log.error("推送消息失败: receiverId={}, error={}", receiverId, e.getMessage());
                    }
                } else {
                    log.warn("用户不在线，无法推送消息: receiverId={}", receiverId);
                }
            } else {
                log.warn("receiverId为空，无法推送消息");
            }
        } catch (Exception e) {
            log.error("处理消息错误: {}", e.getMessage(), e);
        }
    }
}
