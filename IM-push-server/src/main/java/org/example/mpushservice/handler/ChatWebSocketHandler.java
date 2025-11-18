package org.example.mpushservice.handler;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 处理器
 * 管理所有客户端连接
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 存储所有在线用户的 WebSocket 会话
    // Key: userId, Value: WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    private final Gson gson = new Gson();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        if (userId != null) {
            sessions.put(userId, session);
            log.info("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());
            log.info("当前在线用户数: {}", sessions.size());
        } else {
            log.warn("无法提取 userId，关闭连接: sessionId={}", session.getId());
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = extractUserId(session);
        log.debug("收到客户端消息: userId={}, message={}", userId, message.getPayload());
        
        // 可以在这里处理客户端发送的消息（如心跳）
        // 目前主要用于服务端推送，客户端消息较少
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = extractUserId(session);
        if (userId != null) {
            sessions.remove(userId);
            log.info("WebSocket 连接关闭: userId={}, sessionId={}, status={}", userId, session.getId(), status);
            log.info("当前在线用户数: {}", sessions.size());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = extractUserId(session);
        log.error("WebSocket 传输错误: userId={}, sessionId={}", userId, session.getId(), exception);
        
        if (session.isOpen()) {
            session.close();
        }
        if (userId != null) {
            sessions.remove(userId);
        }
    }

    /**
     * 向指定用户发送消息
     */
    public boolean sendMessageToUser(String userId, Object message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = gson.toJson(message);
                session.sendMessage(new TextMessage(json));
                log.debug("发送消息到用户: userId={}, message={}", userId, json);
                return true;
            } catch (IOException e) {
                log.error("发送消息失败: userId={}", userId, e);
                return false;
            }
        } else {
            log.debug("用户不在线或会话已关闭: userId={}", userId);
            return false;
        }
    }

    /**
     * 向多个用户发送消息
     */
    public void sendMessageToUsers(Iterable<String> userIds, Object message) {
        for (String userId : userIds) {
            sendMessageToUser(userId, message);
        }
    }

    /**
     * 广播消息给所有在线用户
     */
    public void broadcast(Object message) {
        String json = gson.toJson(message);
        sessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.error("广播消息失败: userId={}", userId, e);
                }
            }
        });
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineUserCount() {
        return sessions.size();
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 从 WebSocket URI 中提取 userId
     */
    private String extractUserId(WebSocketSession session) {
        String uri = session.getUri().toString();
        // URI 格式: /ws/{userId}
        String[] parts = uri.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1].split("\\?")[0]; // 移除查询参数
        }
        return null;
    }
}
