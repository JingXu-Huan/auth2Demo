package org.example.imgroupserver.util;

import com.example.domain.dto.ChatMessage;
import com.example.domain.dto.ChatMessageWithRecipients;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.config.EventRabbitConfig;
import org.example.imgroupserver.mapper.GroupNodeMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 系统事件发布器
 * 使用 RabbitMQ 发送系统事件通知（群成员变更、群解散等）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final GroupNodeMapper groupMapper;

    /**
     * 发送群成员添加事件
     */
    public void publishMemberAddedEvent(String groupId, String operatorName, List<Map<String, Object>> addedMembers) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("operatorName", operatorName);
        eventData.put("targetUsers", addedMembers);

        publishGroupEvent(groupId, "member_added", eventData, EventRabbitConfig.MEMBER_ADDED_ROUTING_KEY);
    }

    /**
     * 发送群成员移除事件
     */
    public void publishMemberRemovedEvent(String groupId, String operatorName, List<Map<String, Object>> removedMembers) {
        publishMemberRemovedEvent(groupId, operatorName, removedMembers, false);
    }

    /**
     * 发送群成员移除事件（支持标记是否主动退出）
     */
    public void publishMemberRemovedEvent(String groupId, String operatorName, List<Map<String, Object>> removedMembers, boolean isVoluntary) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("operatorName", operatorName);
        eventData.put("targetUsers", removedMembers);
        eventData.put("isVoluntary", isVoluntary);

        // 使用 member_removed 事件类型，前端会根据 isVoluntary 和 targetUsers 判断是否是自己被踢出
        publishGroupEvent(groupId, "member_removed", eventData, EventRabbitConfig.MEMBER_REMOVED_ROUTING_KEY);
    }

    /**
     * 发送群解散事件
     */
    public void publishGroupDisbandedEvent(String groupId) {
        publishGroupEvent(groupId, "group_disbanded", new HashMap<>(), EventRabbitConfig.GROUP_DISBANDED_ROUTING_KEY);
    }

    /**
     * 发送群组系统事件到 RabbitMQ
     * 优化：携带受众列表，遵循"业务与通讯分离"原则
     */
    private void publishGroupEvent(String groupId, String eventType, Map<String, Object> eventData, String routingKey) {
        try {
            // 构建系统事件消息
            ChatMessage eventMessage = new ChatMessage();
            eventMessage.setConversationId("GROUP:" + groupId);
            eventMessage.setContentType(ChatMessage.ContentType.SYSTEM);
            eventMessage.setChannelType(ChatMessage.ChannelType.GROUP);
            eventMessage.setSenderId("system");
            eventMessage.setGroupId(groupId);

            // 构建 payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "EVENT");
            payload.put("eventType", eventType);
            payload.put("data", eventData);
            eventMessage.setPayload(payload);

            // 查询群成员列表（业务服务负责查询受众）
            List<String> recipientIds = new ArrayList<>();
            try {
                List<Object> members = groupMapper.findMembersWithRoleByGroupId(groupId);
                for (Object obj : members) {
                    if (obj instanceof Map) {
                        Map<String, Object> member = (Map<String, Object>) obj;
                        Object userIdObj = member.get("userId");
                        if (userIdObj != null) {
                            recipientIds.add(userIdObj.toString());
                        }
                    }
                }
                log.info("查询到群成员 {} 个: groupId={}", recipientIds.size(), groupId);
            } catch (Exception e) {
                log.error("查询群成员失败: groupId={}", groupId, e);
            }

            // 对于退出事件，需要同时通知退出者本人
            if ("member_removed".equals(eventType) && eventData.containsKey("targetUsers")) {
                List<Map<String, Object>> targetUsers = (List<Map<String, Object>>) eventData.get("targetUsers");
                for (Map<String, Object> user : targetUsers) {
                    Object userIdObj = user.get("user_id");
                    if (userIdObj != null && !recipientIds.contains(userIdObj.toString())) {
                        recipientIds.add(userIdObj.toString());
                    }
                }
            }

            // 包装消息和受众列表
            ChatMessageWithRecipients messageWithRecipients = new ChatMessageWithRecipients(
                eventMessage,
                recipientIds
            );

            // 发送到 RabbitMQ
            log.info("发送系统事件到 MQ: eventType={}, groupId={}, recipientCount={}, routingKey={}", 
                eventType, groupId, recipientIds.size(), routingKey);

            rabbitTemplate.convertAndSend(
                EventRabbitConfig.EVENT_EXCHANGE,
                routingKey,
                messageWithRecipients
            );

            log.info("系统事件已发送到 MQ: eventType={}, groupId={}, recipientCount={}", 
                eventType, groupId, recipientIds.size());
        } catch (Exception e) {
            log.error("发送系统事件到 MQ 失败: eventType={}, groupId={}", eventType, groupId, e);
        }
    }
}
