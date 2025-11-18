package org.example.mpushservice.listener;

import com.example.domain.dto.ChatMessage;
import com.example.domain.dto.ChatMessageWithRecipients;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mpushservice.config.EventRabbitConfig;
import org.example.mpushservice.service.PushService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 消息监听器
 * 监听 RabbitMQ 队列，接收消息并直接通过 WebSocket 推送
 * 这是推送的唯一入口，不再经过 Redis 或其他中间层
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventMessageListener {

    private final PushService pushService;

    /**
     * 监听系统事件队列
     * 优化：接收携带受众列表的消息，不再需要查询群成员
     * @param messageWithRecipients 携带受众列表的系统事件消息
     */
    @RabbitListener(queues = EventRabbitConfig.EVENT_QUEUE)
    public void handleSystemEvent(ChatMessageWithRecipients messageWithRecipients) {
        try {
            ChatMessage event = messageWithRecipients.getMessage();
            java.util.List<String> recipientIds = messageWithRecipients.getRecipientIds();
            
            log.info("从 MQ 接收到系统事件: conversationId={}, eventType={}, recipientCount={}", 
                event.getConversationId(),
                event.getPayload() != null ? ((java.util.Map<?, ?>)event.getPayload()).get("eventType") : "unknown",
                recipientIds != null ? recipientIds.size() : 0);

            // 直接推送到受众列表，不再查询
            pushService.pushSystemEventWithRecipients(event, recipientIds);

            log.info("系统事件推送完成: conversationId={}, recipientCount={}", 
                event.getConversationId(), recipientIds != null ? recipientIds.size() : 0);
        } catch (Exception e) {
            log.error("推送系统事件失败", e);
            // 这里可以考虑重试机制或死信队列
            throw new RuntimeException("推送系统事件失败", e);
        }
    }

    /**
     * 监听聊天消息队列
     * IM-message-server 已经落库，这里只负责推送
     * 注意：这里直接接收原始 byte[]，手动用 Jackson 解析，避免 Spring 消息转换器类型不匹配问题
     */
    @RabbitListener(
            queues = org.example.mpushservice.config.ChatRabbitConfig.CHAT_QUEUE,
            containerFactory = "rawRabbitListenerContainerFactory"
    )
    public void handleChatMessage(byte[] messageBody) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 先解析为 JsonNode，判断是否包含 recipientIds 字段
            JsonNode root = mapper.readTree(messageBody);

            if (root.has("recipientIds")) {
                // 携带受众列表的消息：ChatMessageWithRecipients
                ChatMessageWithRecipients messageWithRecipients = mapper.readValue(messageBody, ChatMessageWithRecipients.class);
                ChatMessage message = messageWithRecipients.getMessage();
                java.util.List<String> recipientIds = messageWithRecipients.getRecipientIds();

                log.info("从 MQ 接收到带受众列表的消息: messageId={}, recipientCount={}",
                    message != null ? message.getMessageId() : null,
                    recipientIds != null ? recipientIds.size() : 0);

                pushService.pushChatMessageWithRecipients(message, recipientIds);

                log.info("消息推送完成: messageId={}, recipientCount={}",
                    message != null ? message.getMessageId() : null,
                    recipientIds != null ? recipientIds.size() : 0);
            } else {
                // 兼容旧格式：仅包含 ChatMessage，本地构造受众列表（单聊场景）
                ChatMessage message = mapper.readValue(messageBody, ChatMessage.class);

                log.info("从 MQ 接收到单聊消息(兼容格式): messageId={}, channelType={}",
                    message.getMessageId(), message.getChannelType());

                java.util.List<String> recipientIds = new java.util.ArrayList<>();
                if (message.getReceiverId() != null) {
                    recipientIds.add(message.getReceiverId());
                }

                pushService.pushChatMessageWithRecipients(message, recipientIds);

                log.info("单聊消息推送完成: messageId={}, recipientCount={}",
                    message.getMessageId(), recipientIds.size());
            }
        } catch (Exception e) {
            log.error("推送聊天消息失败, 原始消息体长度={}",
                messageBody != null ? messageBody.length : -1, e);
            throw new RuntimeException("推送聊天消息失败", e);
        }
    }
}
