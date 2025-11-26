package com.example.imgateway.push;

import com.alibaba.fastjson.JSON;
import com.example.imgateway.session.SessionManager;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 好友通知监听器
 * 
 * 监听 RELATION_SYNC 主题的好友相关消息，直接推送给目标用户
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "RELATION_SYNC",
    consumerGroup = "gateway-friend-notify-group",
    selectorExpression = "FRIEND_REQUEST || FRIEND_ACCEPTED",
    messageModel = MessageModel.BROADCASTING
)
public class FriendNotificationListener implements RocketMQListener<String> {

    @Autowired
    private SessionManager sessionManager;

    @Override
    public void onMessage(String message) {
        try {
            log.info("Gateway收到好友通知: {}", message);
            
            // 尝试解析为好友请求通知
            try {
                FriendRequestNotifyDTO requestDto = JSON.parseObject(message, FriendRequestNotifyDTO.class);
                if (requestDto.getRequesterId() != null && requestDto.getTargetUserId() != null) {
                    handleFriendRequest(requestDto);
                    return;
                }
            } catch (Exception e) {
                // 不是好友请求格式，继续尝试其他格式
                log.debug("不是好友请求格式: {}", e.getMessage());
            }
            
            // 尝试解析为好友接受通知
            try {
                FriendAcceptNotifyDTO acceptDto = JSON.parseObject(message, FriendAcceptNotifyDTO.class);
                if (acceptDto.getRequesterId() != null && acceptDto.getAccepterId() != null) {
                    handleFriendAccepted(acceptDto);
                    return;
                }
            } catch (Exception e) {
                log.error("无法解析好友通知消息: {}", message, e);
            }
            
        } catch (Exception e) {
            log.error("处理好友通知失败: message={}", message, e);
        }
    }
    
    /**
     * 处理好友请求通知
     */
    private void handleFriendRequest(FriendRequestNotifyDTO dto) {
        log.info("处理好友请求通知: requesterId={}, targetUserId={}", 
                dto.getRequesterId(), dto.getTargetUserId());
        
        // 构建推送消息
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "FRIEND_REQUEST");
        notification.put("requesterId", dto.getRequesterId());
        notification.put("targetUserId", dto.getTargetUserId());
        notification.put("message", dto.getMessage() != null ? dto.getMessage() : "请求添加你为好友");
        notification.put("timestamp", System.currentTimeMillis());
        
        // 推送给目标用户
        long targetUserId = dto.getTargetUserId();
        Channel channel = sessionManager.getChannel(targetUserId);
        
        if (channel != null && channel.isActive()) {
            String jsonMessage = JSON.toJSONString(notification);
            channel.writeAndFlush(new TextWebSocketFrame(jsonMessage));
            log.info("好友请求通知已推送: targetUserId={}", targetUserId);
        } else {
            log.debug("目标用户不在当前Gateway节点: targetUserId={}", targetUserId);
        }
    }
    
    /**
     * 处理好友接受通知
     */
    private void handleFriendAccepted(FriendAcceptNotifyDTO dto) {
        log.info("处理好友接受通知: requesterId={}, accepterId={}", dto.getRequesterId(), dto.getAccepterId());
        
        // 构建推送消息，通知请求发起者
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "FRIEND_ACCEPTED");
        notification.put("accepterId", dto.getAccepterId());
        notification.put("requesterId", dto.getRequesterId());
        notification.put("message", "已接受你的好友请求");
        notification.put("timestamp", System.currentTimeMillis());
        
        // 推送给请求发起者
        long requesterId = dto.getRequesterId();
        Channel channel = sessionManager.getChannel(requesterId);
        
        if (channel != null && channel.isActive()) {
            String jsonMessage = JSON.toJSONString(notification);
            channel.writeAndFlush(new TextWebSocketFrame(jsonMessage));
            log.info("好友接受通知已推送: requesterId={}", requesterId);
        } else {
            log.debug("请求发起者不在当前Gateway节点: requesterId={}", requesterId);
        }
    }
    
    /**
     * 好友请求通知 DTO
     */
    @Data
    public static class FriendRequestNotifyDTO {
        private Long requesterId;     // 请求发起者
        private Long targetUserId;    // 目标用户
        private String message;
        private Long tenantId;
    }
    
    /**
     * 好友接受通知 DTO
     */
    @Data
    public static class FriendAcceptNotifyDTO {
        private Long requesterId;     // 原请求发起人
        private Long accepterId;      // 接受人
        private Long tenantId;
    }
    
    /**
     * 好友同步 DTO（旧版，兼容保留）
     */
    @Data
    public static class FriendSyncDTO {
        private Long userId;
        private Long friendId;
        private String action; // ACCEPT
        private Long tenantId;
    }
}
