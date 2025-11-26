package com.example.im.mq;

import com.alibaba.fastjson.JSON;
import com.example.im.dto.MessagePayload;
import com.example.im.entity.MessageInbox;
import com.example.im.mapper.MessageInboxMapper;
import com.example.im.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RocketMQ 事务消息监听器
 * 保证数据库插入与消息投递的原子性
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQTransactionListener
public class MessageTransactionListener implements RocketMQLocalTransactionListener {

    private final MessageMapper messageMapper;
    private final MessageInboxMapper messageInboxMapper;

    /**
     * 执行本地事务（写库）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RocketMQLocalTransactionState executeLocalTransaction(org.springframework.messaging.Message msg, Object arg) {
        log.info("开始执行本地事务, arg={}", arg);
        try {
            MessagePayload payload = (MessagePayload) arg;

            // 1. 插入 Timeline（所有模式必做）
            com.example.im.entity.Message message = toMessageEntity(payload);
            messageMapper.insert(message);
            log.info("消息写入Timeline成功: messageId={}", payload.getMessageId());

            // 2. 如果是写扩散，插入 Inbox
            if (payload.isWriteDiffusion() && payload.getReceiverIds() != null
                    && !payload.getReceiverIds().isEmpty()) {
                List<MessageInbox> inboxList = createInboxEntries(payload);
                messageInboxMapper.batchInsert(inboxList);
                log.info("消息写入Inbox成功: messageId={}, receivers={}",
                        payload.getMessageId(), payload.getReceiverIds().size());
            }

            log.info("本地事务执行成功，返回COMMIT: messageId={}", payload.getMessageId());
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            log.error("消息写入失败，返回ROLLBACK", e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 事务回查（当 Commit 丢失时触发）
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(org.springframework.messaging.Message msg) {
        try {
            String msgIdStr = (String) msg.getHeaders().get("biz_msg_id");
            if (msgIdStr == null) {
                return RocketMQLocalTransactionState.ROLLBACK;
            }

            Long messageId = Long.parseLong(msgIdStr);
            int count = messageMapper.countByMessageId(messageId);

            log.info("事务回查: messageId={}, exists={}", messageId, count > 0);

            return count > 0 ? RocketMQLocalTransactionState.COMMIT
                    : RocketMQLocalTransactionState.ROLLBACK;
        } catch (Exception e) {
            log.error("事务回查失败", e);
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }

    /**
     * 转换为消息实体
     */
    private com.example.im.entity.Message toMessageEntity(MessagePayload payload) {
        com.example.im.entity.Message message = new com.example.im.entity.Message();
        message.setMessageId(payload.getMessageId());
        message.setChannelId(payload.getChannelId());
        message.setSenderId(payload.getSenderId());
        message.setSeqId(payload.getSeqId());
        message.setMsgType(payload.getMsgType());
        message.setContent(payload.getContent());

        if (payload.getMediaUrls() != null) {
            message.setMediaUrls(JSON.toJSONString(payload.getMediaUrls()));
        }

        message.setReplyToMsgId(payload.getReplyToMsgId());
        message.setReplyToUserId(payload.getReplyToUserId());

        if (payload.getMentionedUserIds() != null) {
            message.setMentionedUserIds(JSON.toJSONString(payload.getMentionedUserIds()));
        }

        message.setMentionAll(payload.getMentionAll() != null ? payload.getMentionAll() : false);
        message.setStatus(1); // 正常
        message.setEdited(false);
        message.setExtra(payload.getExtra());
        message.setCreatedAt(payload.getCreatedAt() != null ? payload.getCreatedAt() : OffsetDateTime.now());

        return message;
    }

    /**
     * 创建收件箱条目（写扩散）
     */
    private List<MessageInbox> createInboxEntries(MessagePayload payload) {
        List<MessageInbox> inboxList = new ArrayList<>();
        String preview = getPreview(payload.getContent());
        OffsetDateTime now = OffsetDateTime.now();

        for (Long receiverId : payload.getReceiverIds()) {
            MessageInbox inbox = new MessageInbox();
            inbox.setUserId(receiverId);
            inbox.setMessageId(payload.getMessageId());
            inbox.setChannelId(payload.getChannelId());
            inbox.setSenderId(payload.getSenderId());
            inbox.setMsgType(payload.getMsgType());
            inbox.setPreview(preview);
            inbox.setIsRead(false);
            inbox.setIsDeleted(false);

            // 检查是否被@
            boolean mentioned = payload.getMentionAll() != null && payload.getMentionAll();
            if (!mentioned && payload.getMentionedUserIds() != null) {
                mentioned = payload.getMentionedUserIds().contains(receiverId);
            }
            inbox.setIsMentioned(mentioned);
            inbox.setCreatedAt(now);

            inboxList.add(inbox);
        }

        return inboxList;
    }

    /**
     * 获取消息预览（前100字符）
     */
    private String getPreview(String content) {
        if (content == null) {
            return "";
        }
        return content.length() > 100 ? content.substring(0, 100) : content;
    }
}
