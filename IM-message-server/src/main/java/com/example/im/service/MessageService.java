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
 * ====================================================================
 * 即时通讯消息服务 (IM Message Service)
 * ====================================================================
 * 
 * 【核心职责】
 * IM系统的核心服务，负责消息的完整生命周期管理：
 * - 消息发送与权限校验
 * - 消息ID生成（雪花算法）
 * - 消息持久化存储
 * - 消息推送（通过RocketMQ）
 * 
 * 【消息投递模型 - 混合策略】
 * ┌───────────────────────────────────────────────────────────────┐
 * │                    消息投递策略选择                            │
 * │                                                               │
 * │   群成员数 < 500                    群成员数 >= 500            │
 * │        ↓                                ↓                    │
 * │   写扩散 (Inbox)                   读扩散 (Timeline)          │
 * │        ↓                                ↓                    │
 * │   为每个成员写入消息              只写入一份到群消息表          │
 * │   用户拉取自己收件箱              用户拉取时聚合多个群消息      │
 * │                                                               │
 * │   优点：拉取快                     优点：写入快                 │
 * │   缺点：写入压力大                 缺点：拉取需要聚合           │
 * └───────────────────────────────────────────────────────────────┘
 * 
 * 【消息ID生成 - 雪花算法】
 * 64位ID = 1位符号 + 41位时间戳 + 10位机器ID + 12位序列号
 * - 每毫秒可生成4096个ID
 * - 支持69年不重复
 * - 天然有序，适合做数据库主键
 * 
 * 【消息可靠性 - 事务消息】
 * 使用RocketMQ事务消息保证：消息存储 和 消息推送 的一致性
 * 
 * 1. 发送半消息（Half Message）
 * 2. 执行本地事务（存储消息）
 * 3. 根据本地事务结果提交/回滚
 * 
 * @author 学习笔记
 * @see SnowflakeIdWorker 雪花算法ID生成器
 * @see RocketMQTemplate RocketMQ消息模板
 */
@Slf4j
@Service
@RequiredArgsConstructor  // Lombok: 为final字段生成构造函数
public class MessageService {

    /** 消息数据访问 - 操作messages表 */
    private final MessageMapper messageMapper;
    
    /** 会话/频道数据访问 - 操作channels表 */
    private final ChannelMapper channelMapper;
    
    /** 会话成员数据访问 - 用于权限校验和成员列表 */
    private final ChannelMemberMapper channelMemberMapper;
    
    /** 序列号服务 - 为每个会话生成递增的消息序号 */
    private final SequenceService sequenceService;
    
    /**
     * RocketMQ消息模板
     * 
     * 【RocketMQ简介】
     * 阿里开源的分布式消息中间件，支持：
     * - 事务消息（本项目使用）
     * - 顺序消息
     * - 延迟消息
     * - 批量消息
     */
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 写扩散阈值
     * 
     * 【策略说明】
     * - 群成员 < 500: 写扩散，为每个成员写入消息副本
     * - 群成员 >= 500: 读扩散，只存储一份，读取时聚合
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
