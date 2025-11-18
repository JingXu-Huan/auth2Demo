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
            String receiverId=chatMessage.getReceiverId();
            
            log.info("准备推送消息给用户: receiverId={}, senderId={}", receiverId, chatMessage.getSenderId());
            log.info("当前在线用户列表: {}", sessionManager.getOnlineUsers());

            if(receiverId!=null){
                // 检查用户是否在线
                if (sessionManager.getOnlineUsers().contains(receiverId)) {
                    try {
                        sessionManager.sendMessageToUser(receiverId,messageJson);
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
