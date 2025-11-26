package com.example.im.service;

import com.alibaba.fastjson.JSON;
import com.example.im.dto.MessagePayload;
import com.example.im.dto.SendMessageRequest;
import com.example.im.entity.Channel;
import com.example.im.entity.Message;
import com.example.im.mapper.ChannelMapper;
import com.example.im.mapper.ChannelMemberMapper;
import com.example.im.mapper.MessageMapper;
import com.example.im.util.SnowflakeIdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 消息服务
 * 负责消息的发送、存储、同步
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final ChannelMapper channelMapper;
    private final ChannelMemberMapper channelMemberMapper;
    private final SequenceService sequenceService;
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 写扩散阈值（小于此值采用写扩散）
     */
    private static final int WRITE_DIFFUSION_THRESHOLD = 500;

    /**
     * 发送消息
     * @param senderId 发送者ID
     * @param request 发送请求
     * @return 消息ID
     */
    public Long sendMessage(Long senderId, SendMessageRequest request) {
        // 1. 权限校验
        checkPermission(senderId, request.getChannelId());

        // 2. 获取会话信息
        Channel channel = channelMapper.selectById(request.getChannelId());
        if (channel == null || channel.getStatus() != 1) {
            throw new RuntimeException("会话不存在或已解散");
        }

        // 3. 生成消息ID和序号
        Long messageId = SnowflakeIdWorker.nextId();
        Long seqId = sequenceService.getNextSeqId(request.getChannelId());

        // 4. 构建消息负载
        MessagePayload payload = buildPayload(messageId, seqId, senderId, request, channel);

        // 5. 发送事务消息
        sendTransactionalMessage(payload);

        log.info("消息发送成功: messageId={}, channelId={}, senderId={}, seqId={}",
                messageId, request.getChannelId(), senderId, seqId);

        return messageId;
    }

    /**
     * 构建消息负载
     */
    private MessagePayload buildPayload(Long messageId, Long seqId, Long senderId,
                                        SendMessageRequest request, Channel channel) {
        MessagePayload payload = new MessagePayload();
        payload.setMessageId(messageId);
        payload.setChannelId(request.getChannelId());
        payload.setSenderId(senderId);
        payload.setSeqId(seqId);
        payload.setMsgType(request.getMsgType());
        payload.setContent(request.getContent());
        payload.setMediaUrls(request.getMediaUrls());
        payload.setReplyToMsgId(request.getReplyToMsgId());
        payload.setMentionedUserIds(request.getMentionedUserIds());
        payload.setMentionAll(request.getMentionAll());
        payload.setExtra(request.getExtra());
        payload.setCreatedAt(OffsetDateTime.now());

        // 判断是否采用写扩散
        boolean isWriteDiffusion = channel.getMemberCount() != null
                && channel.getMemberCount() < WRITE_DIFFUSION_THRESHOLD;
        payload.setWriteDiffusion(isWriteDiffusion);

        // 如果是写扩散，获取所有成员ID
        if (isWriteDiffusion) {
            List<Long> memberIds = channelMemberMapper.getMemberIds(request.getChannelId());
            // 排除发送者自己
            memberIds.remove(senderId);
            payload.setReceiverIds(memberIds);
        }

        return payload;
    }

    /**
     * 发送事务消息
     */
    private void sendTransactionalMessage(MessagePayload payload) {
        log.info("准备发送事务消息: messageId={}, channelId={}, receiverIds={}", 
                payload.getMessageId(), payload.getChannelId(), payload.getReceiverIds());
        
        org.springframework.messaging.Message<String> message = MessageBuilder
                .withPayload(JSON.toJSONString(payload))
                .setHeader("biz_msg_id", payload.getMessageId())
                .build();

        try {
            // 发送事务消息到 IM_PUSH_TOPIC
            rocketMQTemplate.sendMessageInTransaction("IM_PUSH_TOPIC", message, payload);
            log.info("事务消息发送完成: messageId={}", payload.getMessageId());
        } catch (Exception e) {
            log.error("事务消息发送失败: messageId={}", payload.getMessageId(), e);
            throw e;
        }
    }

    /**
     * 权限校验
     */
    private void checkPermission(Long userId, Long channelId) {
        // 检查用户是否在会话中
        if (!channelMemberMapper.isMember(channelId, userId)) {
            throw new RuntimeException("您不是该会话的成员");
        }
        // TODO: 检查是否被禁言
    }

    /**
     * 同步消息（拉取）
     * @param userId 用户ID
     * @param channelId 会话ID
     * @param cursor 游标（最后一条消息的seqId）
     * @param limit 数量限制
     * @return 消息列表
     */
    public List<Message> syncMessages(Long userId, Long channelId, Long cursor, Integer limit) {
        // 检查权限
        if (!channelMemberMapper.isMember(channelId, userId)) {
            throw new RuntimeException("您不是该会话的成员");
        }

        // 查询消息（从Timeline）
        return messageMapper.findByChannelIdAndSeqGreaterThan(channelId, cursor, limit);
    }

    /**
     * 撤回消息
     */
    public void recallMessage(Long userId, Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("消息不存在");
        }

        // 检查是否是发送者或管理员
        if (!message.getSenderId().equals(userId)) {
            Integer role = channelMemberMapper.getMemberRole(message.getChannelId(), userId);
            if (role == null || role < 2) {
                throw new RuntimeException("无权撤回此消息");
            }
        }

        // 更新消息状态
        message.setStatus(2); // 已撤回
        messageMapper.updateById(message);

        log.info("消息撤回成功: messageId={}, userId={}", messageId, userId);
    }
}
