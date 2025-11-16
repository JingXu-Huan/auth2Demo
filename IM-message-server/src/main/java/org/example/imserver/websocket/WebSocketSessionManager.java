package org.example.imserver.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
*  @author Junjie
*  @date 2025-11-14
*  @version 1.0
*
 */
@Component
public class WebSocketSessionManager {
    /*
    一个userId对应一个websocket绘画,这是一个本地缓存
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /*
    添加一个本地websocket会话
     */
    public void addSession(String userId,WebSocketSession session) {
        sessions.put(userId, session);
    }

    /*
    删除一个指定的本地会话
     */
    public void removeSession(String userId) {
        sessions.remove(userId);
    }

    /*
    获取一个对话实例
     */
    public WebSocketSession getSession(String userId) {
        return sessions.get(userId);
    }

    /**
     * (辅助方法) 向本地的特定用户发送消息
     */
    public void sendMessageToUser(String userId, String message) throws IOException {
        WebSocketSession session = getSession(userId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new org.springframework.web.socket.TextMessage(message));
        }
    }
}
