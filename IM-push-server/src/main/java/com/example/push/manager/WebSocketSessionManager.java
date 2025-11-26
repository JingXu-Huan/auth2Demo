package com.example.push.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 会话管理器
 * 管理用户的WebSocket连接，支持多设备登录
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    /**
     * 用户ID -> 会话集合（支持多设备）
     */
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(Long userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.debug("添加会话: userId={}, totalSessions={}", userId, userSessions.get(userId).size());
    }

    /**
     * 移除会话
     */
    public void removeSession(Long userId, String sessionId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.removeIf(s -> s.getId().equals(sessionId));
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
            log.debug("移除会话: userId={}, sessionId={}", userId, sessionId);
        }
    }

    /**
     * 获取用户所有会话
     */
    public Set<WebSocketSession> getSessions(Long userId) {
        return userSessions.getOrDefault(userId, Collections.emptySet());
    }

    /**
     * 检查用户是否在线
     */
    public boolean isOnline(Long userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * 向用户发送消息（所有设备）
     */
    public void sendToUser(Long userId, String message) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("用户不在线: userId={}", userId);
            return;
        }

        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("发送消息失败: userId={}, sessionId={}", userId, session.getId(), e);
                }
            }
        }
    }

    /**
     * 向多个用户发送消息
     */
    public void sendToUsers(Set<Long> userIds, String message) {
        for (Long userId : userIds) {
            sendToUser(userId, message);
        }
    }

    /**
     * 广播消息（所有在线用户）
     */
    public void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        userSessions.values().forEach(sessions -> {
            sessions.forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.error("广播消息失败: sessionId={}", session.getId(), e);
                    }
                }
            });
        });
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return userSessions.size();
    }

    /**
     * 获取总连接数
     */
    public int getTotalSessionCount() {
        return userSessions.values().stream()
                .mapToInt(Set::size)
                .sum();
    }
}
