package org.example.imserver.websocket;

import com.example.domain.dto.ChatMessage;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.example.imserver.service.ChatService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;


@Slf4j
@Component
public class MyWebSocketHandler extends TextWebSocketHandler {
    private final WebSocketSessionManager sessionManager;
    private final ChatService chatService;
    private final StringRedisTemplate redisTemplate;
    private final Gson gson=new Gson();

    private static final String ONLINE_USERS_KEY = "global:online:users";

    public MyWebSocketHandler(WebSocketSessionManager sessionManager, ChatService chatService, StringRedisTemplate redisTemplate)
    {
        this.sessionManager=sessionManager;
        this.chatService=chatService;
        this.redisTemplate = redisTemplate;
    }

    /*
    处理用户连接
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            String userId = getUserIdFromSession(session);
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("WebSocket连接缺少用户ID，关闭连接");
                session.close(CloseStatus.BAD_DATA.withReason("Missing user ID"));
                return;
            }
            sessionManager.addSession(userId, session);
            
            // 暂时注释Redis操作，避免连接失败
            try {
                redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);
            } catch (Exception redisError) {
                log.warn("Redis操作失败，但WebSocket连接正常: {}", redisError.getMessage());
            }
            
            log.info("ID为:{}的用户建立连接! 当前在线用户数: {}", userId, sessionManager.getOnlineUserCount());
        } catch (Exception e) {
            log.error("建立WebSocket连接时发生异常: {}", e.getMessage(), e);
            session.close(CloseStatus.SERVER_ERROR.withReason("Connection error"));
        }
    }

    /*
    处理消息
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String userId = getUserIdFromSession(session);
            if (userId == null) {
                log.warn("收到消息但无法获取用户ID，忽略消息");
                return;
            }
            
            ChatMessage chatMessage = gson.fromJson(message.getPayload(), ChatMessage.class);
            if (chatMessage == null) {
                log.warn("收到无效的消息格式，用户ID: {}", userId);
                return;
            }
            
            chatMessage.setSenderId(userId);
            
            // 如果是群聊消息，直接广播给所有在线用户
            if (ChatMessage.ChannelType.GROUP.equals(chatMessage.getChannelType())) {
                // 广播给所有在线用户（除了发送者）
                sessionManager.getOnlineUsers().forEach(onlineUserId -> {
                    if (!onlineUserId.equals(userId)) {
                        try {
                            sessionManager.sendMessageToUser(onlineUserId, message.getPayload());
                        } catch (Exception e) {
                            log.warn("向用户{}发送消息失败: {}", onlineUserId, e.getMessage());
                        }
                    }
                });
                log.debug("广播群聊消息，发送者: {}", userId);
            } else {
                // 单聊消息，使用原有逻辑
                chatService.handleMessage(chatMessage);
                log.debug("处理用户{}的单聊消息", userId);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息时发生异常: {}", e.getMessage(), e);
        }
    }

    /*
    处理用户断开连接
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session,CloseStatus status) throws Exception {
        String userId=getUserIdFromSession(session);
        if(userId!=null)
        {
            sessionManager.removeSession(userId);
            
            // 暂时注释Redis操作，避免连接失败
            try {
                redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
            } catch (Exception redisError) {
                log.warn("Redis操作失败: {}", redisError.getMessage());
            }
            
            log.info("ID为:{}的用户断开连接! 当前在线用户数: {}", userId, sessionManager.getOnlineUserCount());
        }
    }

    /*
    处理传输session传输错误
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Redis Session传输错误:{}",session.getId(),exception);
        System.err.println("Transport error for session " + session.getId() + ": " + exception.getMessage());
        afterConnectionClosed(session, CloseStatus.NO_STATUS_CODE);
    }


    private String getUserIdFromSession(WebSocketSession session) {
        // 从 /ws/{userId} 中解析 userId
        UriComponents uriComponents = UriComponentsBuilder.fromUri(session.getUri()).build();
        // 路径片段，例如 [ "ws", "123" ]
        String path = uriComponents.getPath();
        if (path != null) {
            String[] segments = path.split("/");
            if (segments.length > 1) {
                // 返回最后一个片段
                return segments[segments.length - 1];
            }
        }
        //备用
        return (String) session.getAttributes().get("userId");
    }


}
