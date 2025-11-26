package com.example.av.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * WebRTC信令WebSocket处理器
 */
@Slf4j
@Component
public class SignalingHandler extends TextWebSocketHandler {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // userId -> WebSocketSession
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    // sessionId -> userId
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("信令WebSocket连接建立: {}", session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");
            
            switch (type) {
                case "register" -> handleRegister(session, payload);
                case "offer", "answer", "ice-candidate" -> handleSignaling(session, payload);
                case "heartbeat" -> handleHeartbeat(session);
                default -> log.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理信令消息失败", e);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
            log.info("信令WebSocket连接关闭: userId={}", userId);
        }
    }
    
    /**
     * 处理注册消息
     */
    private void handleRegister(WebSocketSession session, Map<String, Object> payload) {
        Long userId = ((Number) payload.get("userId")).longValue();
        userSessions.put(userId, session);
        sessionUserMap.put(session.getId(), userId);
        log.info("用户注册信令通道: userId={}", userId);
        
        // 发送注册成功响应
        sendToSession(session, Map.of("type", "registered", "userId", userId));
    }
    
    /**
     * 处理信令转发
     */
    private void handleSignaling(WebSocketSession session, Map<String, Object> payload) {
        Long toUserId = ((Number) payload.get("to")).longValue();
        sendToUser(toUserId, payload);
    }
    
    /**
     * 处理心跳
     */
    private void handleHeartbeat(WebSocketSession session) {
        sendToSession(session, Map.of("type", "heartbeat-ack"));
    }
    
    /**
     * 发送消息给指定用户
     */
    public void sendToUser(Long userId, Map<String, Object> message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            sendToSession(session, message);
        } else {
            log.warn("用户不在线或连接已关闭: userId={}", userId);
        }
    }
    
    /**
     * 发送消息给指定会话
     */
    private void sendToSession(WebSocketSession session, Map<String, Object> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
}
