package com.example.push.handler;

import com.alibaba.fastjson.JSON;
import com.example.push.manager.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * WebSocket 消息处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserId(session);
        if (userId != null) {
            sessionManager.addSession(userId, session);
            log.info("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());
        } else {
            log.warn("WebSocket 连接失败: 未获取到用户ID");
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = getUserId(session);
        String payload = message.getPayload();
        log.debug("收到消息: userId={}, payload={}", userId, payload);

        // 解析消息
        try {
            Map<String, Object> msg = JSON.parseObject(payload, Map.class);
            String type = (String) msg.get("type");

            switch (type) {
                case "ping":
                    // 心跳响应
                    session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
                    break;
                case "ack":
                    // 消息确认
                    handleAck(userId, msg);
                    break;
                default:
                    log.debug("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("消息处理失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserId(session);
        if (userId != null) {
            sessionManager.removeSession(userId, session.getId());
            log.info("WebSocket 连接关闭: userId={}, sessionId={}, status={}",
                    userId, session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = getUserId(session);
        log.error("WebSocket 传输错误: userId={}, error={}", userId, exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    /**
     * 从Session中获取用户ID
     */
    private Long getUserId(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        Object userId = attributes.get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    /**
     * 处理消息确认
     */
    private void handleAck(Long userId, Map<String, Object> msg) {
        Object messageId = msg.get("messageId");
        if (messageId != null) {
            log.debug("消息确认: userId={}, messageId={}", userId, messageId);
            // TODO: 更新消息状态
        }
    }
}
