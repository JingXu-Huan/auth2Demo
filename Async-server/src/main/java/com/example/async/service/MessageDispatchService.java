package com.example.async.service;

import com.example.async.entity.LocalMessage;
import com.example.async.mapper.LocalMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 消息分发服务
 * 使用Java 21虚拟线程处理消息发送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDispatchService {

    private final LocalMessageMapper localMessageMapper;
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 使用 Java 21 虚拟线程池
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 定时扫描待发送消息（每5秒）
     */
    @Scheduled(fixedDelay = 5000)
    public void scanAndDispatch() {
        List<LocalMessage> pendingMessages = localMessageMapper.findPendingMessages(
                LocalDateTime.now(), 100);

        if (pendingMessages.isEmpty()) {
            return;
        }

        log.info("扫描到 {} 条待发送消息", pendingMessages.size());

        for (LocalMessage message : pendingMessages) {
            // 使用虚拟线程异步发送
            executor.submit(() -> dispatchMessage(message));
        }
    }

    /**
     * 发送单条消息
     */
    private void dispatchMessage(LocalMessage message) {
        try {
            String destination = message.getTag() != null
                    ? message.getTopic() + ":" + message.getTag()
                    : message.getTopic();

            rocketMQTemplate.send(destination,
                    MessageBuilder.withPayload(message.getPayload())
                            .setHeader("KEYS", message.getKeys())
                            .setHeader("messageId", message.getMessageId())
                            .build());

            // 标记为已发送
            localMessageMapper.markAsSent(message.getId(), LocalDateTime.now());
            log.debug("消息发送成功: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("消息发送失败: messageId={}", message.getMessageId(), e);

            // 计算下次重试时间（指数退避）
            int retryCount = message.getRetryCount() + 1;
            int delaySeconds = (int) Math.pow(2, retryCount) * 10; // 10s, 20s, 40s, 80s...
            LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(delaySeconds);

            localMessageMapper.markAsFailed(
                    message.getId(),
                    nextRetryAt,
                    "SEND_ERROR",
                    e.getMessage(),
                    LocalDateTime.now()
            );
        }
    }

    /**
     * 保存本地消息
     */
    public void saveLocalMessage(String topic, String tag, String keys,
                                  String payload, String businessKey,
                                  String businessType, String businessId) {
        LocalMessage message = new LocalMessage();
        message.setMessageId(java.util.UUID.randomUUID().toString());
        message.setTopic(topic);
        message.setTag(tag);
        message.setKeys(keys);
        message.setPayload(payload);
        message.setBusinessKey(businessKey);
        message.setBusinessType(businessType);
        message.setBusinessId(businessId);
        message.setStatus(0); // 待发送
        message.setRetryCount(0);
        message.setMaxRetry(3);
        message.setCreatedAt(LocalDateTime.now());

        localMessageMapper.insert(message);
        log.info("本地消息已保存: messageId={}, topic={}", message.getMessageId(), topic);
    }

    /**
     * 确认消息已消费
     */
    public void confirmMessage(String messageId) {
        localMessageMapper.markAsConfirmed(messageId, LocalDateTime.now());
        log.debug("消息已确认: messageId={}", messageId);
    }
}
