package org.example.imserver.websocket;

import com.example.domain.dto.ChatMessage;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

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

            if(receiverId!=null){
                sessionManager.sendMessageToUser(receiverId,messageJson);
            }
        } catch (IOException e) {
            System.err.println("RedisPublisher发送消息失败:{} " + e.getMessage());
        } catch (Exception e) {
            System.err.println("RedisPublisher错误:{} " + e.getMessage());
        }
    }
}
