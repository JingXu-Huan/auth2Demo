package com.example.imgateway.push;

import com.example.im.protocol.IMProtocol;
import com.example.imgateway.session.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 消息推送监听器
 *
 * IM-Service 将下行消息写入 RocketMQ Topic（JSON格式），
 * 本监听器使用广播模式订阅，并根据 receiverId 将消息推送到对应用户的Channel。
 * 推送时转换为 Protobuf 格式。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "IM_PUSH_TOPIC",
        consumerGroup = "im-gateway-consumer",
        messageModel = MessageModel.BROADCASTING
)
public class MessagePushListener implements RocketMQListener<String> {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(String message) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("收到MQ推送消息: {}", message);
            }

            JsonNode root = objectMapper.readTree(message);
            
            // 解析消息字段
            long senderId = root.has("senderId") ? root.get("senderId").asLong() : 0;
            long channelId = root.has("channelId") ? root.get("channelId").asLong() : 0;
            long messageId = root.has("messageId") ? root.get("messageId").asLong() : 0;
            int msgType = root.has("msgType") ? root.get("msgType").asInt() : 1;
            String content = root.has("content") ? root.get("content").asText() : "";
            long timestamp = root.has("createdAt") ? System.currentTimeMillis() : System.currentTimeMillis();
            
            // 支持 receiverIds 数组（写扩散模式）
            JsonNode receiverIdsNode = root.get("receiverIds");
            if (receiverIdsNode != null && receiverIdsNode.isArray() && receiverIdsNode.size() > 0) {
                for (JsonNode receiverNode : receiverIdsNode) {
                    long targetUserId = receiverNode.asLong();
                    pushToUser(targetUserId, senderId, channelId, messageId, msgType, content, timestamp);
                }
                return;
            }
            
            // 兼容旧的 receiverId 单个用户（读扩散模式）
            JsonNode receiverNode = root.get("receiverId");
            if (receiverNode != null && !receiverNode.isNull()) {
                long targetUserId = receiverNode.asLong();
                pushToUser(targetUserId, senderId, channelId, messageId, msgType, content, timestamp);
                return;
            }
            
            // 如果都没有，推送给发送者自己（确认消息）
            if (senderId > 0) {
                pushToUser(senderId, senderId, channelId, messageId, msgType, content, timestamp);
            }
        } catch (Exception e) {
            log.error("处理MQ推送消息失败, message={} ", message, e);
        }
    }
    
    /**
     * 推送消息到指定用户（Protobuf 格式）
     */
    private void pushToUser(long targetUserId, long senderId, long channelId, 
                           long messageId, int msgType, String content, long timestamp) {
        Channel channel = sessionManager.getChannel(targetUserId);

        if (channel != null && channel.isActive()) {
            // 构建 Protobuf 消息
            IMProtocol.PushMessage pushMessage = IMProtocol.PushMessage.newBuilder()
                    .setSenderId(senderId)
                    .setReceiverId(targetUserId)
                    .setGroupId(channelId)  // 使用 groupId 存储 channelId
                    .setMsgType(msgType)
                    .setContent(content)
                    .setMsgId(messageId)
                    .setTimestamp(timestamp)
                    .build();
            
            IMProtocol.Header header = IMProtocol.Header.newBuilder()
                    .setCommand(IMProtocol.CommandType.MSG_PUSH_VALUE)
                    .setVersion(1)
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            
            IMProtocol.IMPacket packet = IMProtocol.IMPacket.newBuilder()
                    .setHeader(header)
                    .setBody(ByteString.copyFrom(pushMessage.toByteArray()))
                    .build();
            
            channel.writeAndFlush(packet);
            log.info("消息已推送到用户(Protobuf): userId={}, channelId={}, msgId={}", 
                    targetUserId, channel.id().asShortText(), messageId);
        } else {
            // 用户不在当前节点，其他Gateway节点会处理（广播模式）
            if (log.isDebugEnabled()) {
                log.debug("用户不在线或不在当前节点: userId={}", targetUserId);
            }
        }
    }
}
