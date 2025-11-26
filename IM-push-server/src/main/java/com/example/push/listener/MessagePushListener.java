package com.example.push.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.push.manager.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RocketMQ 消息推送监听器
 * 消费 IM_PUSH_TOPIC 消息并推送给在线用户
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "IM_PUSH_TOPIC",
        consumerGroup = "im_push_consumer_group"
)
public class MessagePushListener implements RocketMQListener<String> {

    private final WebSocketSessionManager sessionManager;

    @Override
    public void onMessage(String message) {
        try {
            log.info("收到RocketMQ推送消息: {}", message);
            
            JSONObject payload = JSON.parseObject(message);
            Long channelId = payload.getLong("channelId");
            Long senderId = payload.getLong("senderId");
            Long messageId = payload.getLong("messageId");
            Long seqId = payload.getLong("seqId");
            
            // 获取接收者列表
            List<Long> receiverIds = payload.getJSONArray("receiverIds") != null
                    ? payload.getJSONArray("receiverIds").toJavaList(Long.class)
                    : null;

            // 构建推送消息
            JSONObject pushMessage = new JSONObject();
            pushMessage.put("type", "new_message");
            pushMessage.put("channelId", channelId);
            pushMessage.put("messageId", messageId);
            pushMessage.put("seqId", seqId);
            pushMessage.put("senderId", senderId);
            pushMessage.put("msgType", payload.getInteger("msgType"));
            pushMessage.put("content", payload.getString("content"));
            pushMessage.put("createdAt", payload.getString("createdAt"));
            
            String pushJson = pushMessage.toJSONString();

            // 推送消息
            if (receiverIds != null && !receiverIds.isEmpty()) {
                // 写扩散模式：推送给指定接收者
                Set<Long> receivers = new HashSet<>(receiverIds);
                sessionManager.sendToUsers(receivers, pushJson);
                log.info("消息推送完成(写扩散): messageId={}, receivers={}", messageId, receivers.size());
            } else {
                // 读扩散模式：需要查询群成员（简化处理：发送给发送者确认）
                sessionManager.sendToUser(senderId, pushJson);
                log.info("消息推送完成(读扩散): messageId={}, senderId={}", messageId, senderId);
            }
        } catch (Exception e) {
            log.error("消息推送失败", e);
        }
    }
}
