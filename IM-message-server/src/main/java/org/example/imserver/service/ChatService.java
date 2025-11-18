package org.example.imserver.service;

import com.example.domain.dto.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import com.example.domain.dto.ForwardMessageRequest;
import org.example.imserver.exception.ChatException;
import com.example.domain.model.ChatMessageDocument;
import com.example.domain.model.DeletedMessageDocument;
import com.example.domain.model.FavoriteMessageDocument;
import com.example.domain.model.MessageReadReceiptDocument;
import com.example.domain.model.PinnedMessageDocument;
import org.example.imserver.mapper.ChatMessageMapper;
import org.example.imserver.mapper.DeletedMessageMapper;
import org.example.imserver.mapper.FavoriteMessageMapper;
import org.example.imserver.mapper.MessageReadReceiptMapper;
import org.example.imserver.mapper.PinnedMessageMapper;
import org.example.imserver.websocket.RedisMessagePublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 处理来自 WebSocket 的消息
 */
@Slf4j
@Service
public class ChatService {

    private final RedisMessagePublisher publisher;
    private final RabbitTemplate rabbitTemplate;
    private final ChatMessageMapper chatMessageMapper;
    private final DeletedMessageMapper deletedMessageMapper;
    private final FavoriteMessageMapper favoriteMessageMapper;
    private final PinnedMessageMapper pinnedMessageMapper;
    private final MessageReadReceiptMapper messageReadReceiptMapper;

    public ChatService(RedisMessagePublisher publisher,
                      RabbitTemplate rabbitTemplate,
                      ChatMessageMapper chatMessageMapper,
                      DeletedMessageMapper deletedMessageMapper,
                      FavoriteMessageMapper favoriteMessageMapper,
                      PinnedMessageMapper pinnedMessageMapper,
                      MessageReadReceiptMapper messageReadReceiptMapper) {
        this.publisher = publisher;
        this.rabbitTemplate = rabbitTemplate;
        this.chatMessageMapper = chatMessageMapper;
        this.deletedMessageMapper = deletedMessageMapper;
        this.favoriteMessageMapper = favoriteMessageMapper;
        this.pinnedMessageMapper = pinnedMessageMapper;
        this.messageReadReceiptMapper = messageReadReceiptMapper;
    }

    /**
     * 主入口：处理来自 WebSocket 的消息
     */
    public void handleMessage(ChatMessage chatMessage) {
        try {
            log.info("开始处理消息: senderId={}, receiverId={}, channelType={}, contentType={}", 
                chatMessage.getSenderId(), chatMessage.getReceiverId(), 
                chatMessage.getChannelType(), chatMessage.getContentType());
            
            // 验证消息
            validateMessage(chatMessage);
            enrichMessage(chatMessage);
            persistMessage(chatMessage);
            
            log.info("消息已持久化: messageId={}, conversationId={}", 
                chatMessage.getMessageId(), chatMessage.getConversationId());

            // 发布消息到 RabbitMQ，由 IM-push-server 推送
            if (chatMessage.getChannelType() == ChatMessage.ChannelType.PRIVATE) {
                log.info("发布单聊消息到 MQ: senderId={}, receiverId={}", 
                    chatMessage.getSenderId(), chatMessage.getReceiverId());
                rabbitTemplate.convertAndSend(
                    org.example.imserver.config.ChatRabbitConfig.CHAT_EXCHANGE,
                    org.example.imserver.config.ChatRabbitConfig.PRIVATE_MESSAGE_ROUTING_KEY,
                    chatMessage
                );
                log.info("单聊消息已发布到 MQ: {} to {}",
                    chatMessage.getSenderId(), chatMessage.getReceiverId());
            } else if (chatMessage.getChannelType() == ChatMessage.ChannelType.GROUP) {
                log.info("发布群聊消息到 MQ: senderId={}, groupId={}",
                    chatMessage.getSenderId(), chatMessage.getGroupId());
                rabbitTemplate.convertAndSend(
                    org.example.imserver.config.ChatRabbitConfig.CHAT_EXCHANGE,
                    org.example.imserver.config.ChatRabbitConfig.GROUP_MESSAGE_ROUTING_KEY,
                    chatMessage
                );
                log.info("群聊消息已发布到 MQ: senderId={}, groupId={}",
                    chatMessage.getSenderId(), chatMessage.getGroupId());
            }
        } catch (Exception e) {
            log.error("处理消息失败: {}", e.getMessage(), e);
            throw new ChatException("MESSAGE_HANDLE_ERROR", "消息处理失败: " + e.getMessage(), e);
        }
    }

    private void persistMessage(ChatMessage message) {
        ChatMessageDocument document = toDocument(message);
        chatMessageMapper.save(document);
    }

    private ChatMessageDocument toDocument(ChatMessage message) {
        ChatMessageDocument document = new ChatMessageDocument();
        document.setMessageId(message.getMessageId());
        document.setClientMsgId(message.getClientMsgId());
        document.setConversationId(message.getConversationId());
        document.setSeq(message.getSeq());
        document.setStatus(message.getStatus());
        document.setCreatedAt(message.getCreatedAt());
        document.setSenderId(message.getSenderId());
        document.setReceiverId(message.getReceiverId());
        document.setGroupId(message.getGroupId());
        document.setChannelType(message.getChannelType());
        document.setContentType(message.getContentType());
        document.setPayload(message.getPayload());
        document.setRecalledAt(message.getRecalledAt());
        document.setReplyToMessageId(message.getReplyToMessageId());
        return document;
    }

    private ChatMessage fromDocument(ChatMessageDocument document) {
        ChatMessage message = new ChatMessage();
        message.setMessageId(document.getMessageId());
        message.setClientMsgId(document.getClientMsgId());
        message.setConversationId(document.getConversationId());
        message.setSeq(document.getSeq());
        message.setStatus(document.getStatus());
        message.setCreatedAt(document.getCreatedAt());
        message.setSenderId(document.getSenderId());
        message.setReceiverId(document.getReceiverId());
        message.setGroupId(document.getGroupId());
        message.setChannelType(document.getChannelType());
        message.setContentType(document.getContentType());
        message.setPayload(document.getPayload());
        message.setRecalledAt(document.getRecalledAt());
        message.setReplyToMessageId(document.getReplyToMessageId());
        return message;
    }

    public List<ChatMessage> getHistory(String conversationId, Long cursor, int size) {
        if (size <= 0) {
            size = 50;
        }
        Pageable pageable = PageRequest.of(0, size);
        Page<ChatMessageDocument> page;
        if (cursor == null) {
            page = chatMessageMapper.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        } else {
            page = chatMessageMapper.findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(conversationId, cursor, pageable);
        }
        return page.getContent().stream()
                .map(this::fromDocument)
                .collect(Collectors.toList());
    }

    public ChatMessage updateStatus(String messageId, ChatMessage.MessageStatus newStatus) {
        ChatMessageDocument document = chatMessageMapper.findByMessageId(messageId)
                .orElseThrow(() -> new ChatException("MESSAGE_NOT_FOUND", "消息不存在"));
        document.setStatus(newStatus);
        chatMessageMapper.save(document);
        return fromDocument(document);
    }

    /**
     * 撤回指定消息
     *
     * 规则：仅允许在固定时间窗口内（当前为 2 分钟）撤回，超时则抛出业务异常。
     *
     * @param messageId 要撤回的消息ID
     * @return 撤回后的消息对象（状态为 RECALLED，带有 recalledAt 时间戳）
     */
    public ChatMessage recallMessage(String messageId) {
        ChatMessageDocument document = chatMessageMapper.findByMessageId(messageId)
                .orElseThrow(() -> new ChatException("MESSAGE_NOT_FOUND", "消息不存在"));

        Long createdAt = document.getCreatedAt();
        long now = System.currentTimeMillis();
        long windowMillis = 2 * 60 * 1000L;

        if (createdAt != null && now - createdAt > windowMillis) {
            throw new ChatException("RECALL_TIMEOUT", "消息已超出可撤回时间窗口");
        }

        document.setStatus(ChatMessage.MessageStatus.RECALLED);
        document.setRecalledAt(now);
        chatMessageMapper.save(document);
        return fromDocument(document);
    }

    /**
     * 批量将消息状态标记为已读
     *
     * @param messageIds 需要标记为 READ 的消息ID列表
     * @return 更新后的消息列表（如果没有匹配到消息则返回空列表）
     */
    public List<ChatMessage> markMessagesRead(List<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChatMessageDocument> documents = chatMessageMapper.findByMessageIdIn(messageIds);
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        for (ChatMessageDocument document : documents) {
            document.setStatus(ChatMessage.MessageStatus.READ);
        }
        chatMessageMapper.saveAll(documents);
        return documents.stream().map(this::fromDocument).collect(Collectors.toList());
    }

    /**
     * 获取用户的未读消息汇总信息
     *
     * <p>按会话维度聚合未读消息数量，并附带每个会话的最后一条未读消息，用于会话列表展示。</p>
     *
     * @param userId 接收者用户ID
     * @return 包含 totalUnread（未读总数）和 conversations（按会话聚合详情）的 Map
     */
    public Map<String, Object> getUnreadSummary(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new ChatException("INVALID_USER_ID", "userId 不能为空");
        }
        List<ChatMessageDocument> unreadDocuments = chatMessageMapper
                .findByReceiverIdAndStatusNot(userId, ChatMessage.MessageStatus.READ);

        int totalUnread = unreadDocuments.size();

        Map<String, List<ChatMessageDocument>> grouped = unreadDocuments.stream()
                .collect(Collectors.groupingBy(ChatMessageDocument::getConversationId));

        List<Map<String, Object>> conversations = grouped.entrySet().stream()
                .map(entry -> {
                    String conversationId = entry.getKey();
                    List<ChatMessageDocument> docs = entry.getValue();
                    docs.sort((a, b) -> {
                        long ca = a.getCreatedAt() != null ? a.getCreatedAt() : 0L;
                        long cb = b.getCreatedAt() != null ? b.getCreatedAt() : 0L;
                        return Long.compare(cb, ca);
                    });
                    ChatMessageDocument latest = docs.get(0);
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("conversationId", conversationId);
                    conv.put("unreadCount", docs.size());
                    conv.put("lastMessage", fromDocument(latest));
                    return conv;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("totalUnread", totalUnread);
        result.put("conversations", conversations);
        return result;
    }

    /**
     * 基于已有消息进行转发
     *
     * <p>从存储中查出原始消息，根据请求中的目标用户/群组，
     * 为每个目标构造新的 ChatMessage 并复用原始消息的内容载体，
     * 然后复用 handleMessage 流程进行校验、落库与分发。</p>
     *
     * @param request ForwardMessageRequest，包含原消息ID、转发发起人、目标用户ID列表、目标群组ID列表
     * @return Map，包含 forwardedCount（成功转发条数）和 messages（新生成的消息列表）
     */
    public Map<String, Object> forwardMessage(ForwardMessageRequest request) {
        if (request == null || request.getMessageId() == null || request.getMessageId().trim().isEmpty()) {
            throw new ChatException("INVALID_FORWARD_REQUEST", "messageId 不能为空");
        }

        ChatMessageDocument originalDocument = chatMessageMapper.findByMessageId(request.getMessageId())
                .orElseThrow(() -> new ChatException("MESSAGE_NOT_FOUND", "原始消息不存在"));
        ChatMessage original = fromDocument(originalDocument);

        String forwarderId = request.getForwarderId();
        if (forwarderId == null || forwarderId.trim().isEmpty()) {
            forwarderId = original.getSenderId();
        }

        List<String> targetUserIds = request.getTargetUserIds();
        List<String> targetGroupIds = request.getTargetGroupIds();
        boolean hasUserTargets = targetUserIds != null && !targetUserIds.isEmpty();
        boolean hasGroupTargets = targetGroupIds != null && !targetGroupIds.isEmpty();

        if (!hasUserTargets && !hasGroupTargets) {
            throw new ChatException("INVALID_FORWARD_TARGETS", "目标用户或群组不能为空");
        }

        List<ChatMessage> createdMessages = new ArrayList<>();

        // 转发到单聊
        if (hasUserTargets) {
            for (String userId : targetUserIds) {
                ChatMessage forwardMsg = new ChatMessage();
                forwardMsg.setSenderId(forwarderId);
                forwardMsg.setReceiverId(userId);
                forwardMsg.setChannelType(ChatMessage.ChannelType.PRIVATE);
                forwardMsg.setContentType(original.getContentType());
                forwardMsg.setPayload(original.getPayload());
                // 将原消息ID作为引用，便于前端展示“转发自”信息
                forwardMsg.setReplyToMessageId(original.getMessageId());
                handleMessage(forwardMsg);
                createdMessages.add(forwardMsg);
            }
        }

        // 转发到群聊
        if (hasGroupTargets) {
            for (String groupId : targetGroupIds) {
                ChatMessage forwardMsg = new ChatMessage();
                forwardMsg.setSenderId(forwarderId);
                forwardMsg.setGroupId(groupId);
                forwardMsg.setChannelType(ChatMessage.ChannelType.GROUP);
                forwardMsg.setContentType(original.getContentType());
                forwardMsg.setPayload(original.getPayload());
                forwardMsg.setReplyToMessageId(original.getMessageId());
                handleMessage(forwardMsg);
                createdMessages.add(forwardMsg);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("forwardedCount", createdMessages.size());
        result.put("messages", createdMessages);
        return result;
    }

    /**
     * 简单消息搜索
     *
     * <p>基于会话ID进行搜索，可选按发送者、内容类型以及关键字过滤，
     * 关键字目前仅对文本消息（TEXT）的 text 字段进行包含匹配（不区分大小写）。</p>
     *
     * @param conversationId 会话ID（必填）
     * @param senderId       发送者ID（可选）
     * @param contentType    消息内容类型（可选）
     * @param keyword        关键字（可选），仅对文本消息进行匹配
     * @param page           页码，从1开始
     * @param size           每页数量
     * @return Map，包含 total（匹配总数）、page、size、messages（当前页消息列表）
     */
    public Map<String, Object> searchMessages(String conversationId,
                                              String senderId,
                                              ChatMessage.ContentType contentType,
                                              String keyword,
                                              int page,
                                              int size) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new ChatException("INVALID_CONVERSATION_ID", "conversationId 不能为空");
        }
        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 50;
        }

        // 先获取该会话下的全部消息，后续在内存中过滤并手动分页（简单实现，适合初期规模）
        Page<ChatMessageDocument> allPage = chatMessageMapper
                .findByConversationIdOrderByCreatedAtDesc(conversationId, Pageable.unpaged());
        List<ChatMessage> allMessages = allPage.getContent().stream()
                .map(this::fromDocument)
                .collect(Collectors.toList());

        String keywordLower = keyword != null ? keyword.toLowerCase() : null;

        List<ChatMessage> filtered = allMessages.stream()
                .filter(msg -> senderId == null || senderId.equals(msg.getSenderId()))
                .filter(msg -> contentType == null || contentType == msg.getContentType())
                .filter(msg -> {
                    if (keywordLower == null || keywordLower.isEmpty()) {
                        return true;
                    }
                    if (msg.getContentType() != ChatMessage.ContentType.TEXT) {
                        return false;
                    }
                    Object payload = msg.getPayload();
                    String text = null;
                    if (payload instanceof ChatMessage.TextPayload) {
                        text = ((ChatMessage.TextPayload) payload).getText();
                    } else if (payload instanceof Map) {
                        Object textObj = ((Map<?, ?>) payload).get("text");
                        if (textObj != null) {
                            text = textObj.toString();
                        }
                    }
                    return text != null && text.toLowerCase().contains(keywordLower);
                })
                .collect(Collectors.toList());

        int total = filtered.size();
        int fromIndex = Math.max((page - 1) * size, 0);
        int toIndex = Math.min(fromIndex + size, total);
        List<ChatMessage> pageMessages;
        if (fromIndex >= total) {
            pageMessages = Collections.emptyList();
        } else {
            pageMessages = filtered.subList(fromIndex, toIndex);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("messages", pageMessages);
        return result;
    }

    /**
     * 处理群聊的 "Fan-out"（扇出）逻辑
     * 这是此架构中处理群聊的最佳方式
     */
    private void handleGroupMessage(ChatMessage groupMessage) {
        try {

            // 1. 从群组服务获取该群的所有成员
            // List<String> memberIds = groupService.getMemberIds(groupMessage.getGroupId());
            // (Mock 数据)
            List<String> memberIds = Arrays.asList("user1", "user2", "user3", groupMessage.getSenderId()); // 假设群里有这些人

            log.info("Fanning out group message to {} members in group: {}", 
                memberIds.size(), groupMessage.getGroupId());

            // 2. 转换为多条"单聊"消息，发布到 Redis
            // 订阅者不需要知道这是群聊，它只管按 receiverId 投递
            for (String memberId : memberIds) {
                // 不发给自己，避免发送者收到自己的消息
                if (memberId.equals(groupMessage.getSenderId())) continue;

                ChatMessage privateMessage = new ChatMessage();
                privateMessage.setMessageId(groupMessage.getMessageId());
                privateMessage.setClientMsgId(groupMessage.getClientMsgId());
                privateMessage.setConversationId(groupMessage.getConversationId());
                privateMessage.setSeq(groupMessage.getSeq());
                privateMessage.setStatus(groupMessage.getStatus());
                privateMessage.setSenderId(groupMessage.getSenderId());
                privateMessage.setReceiverId(memberId); // 关键：设置接收者
                privateMessage.setPayload(groupMessage.getPayload()); // 使用 payload 而不是 content
                privateMessage.setContentType(groupMessage.getContentType()); // 保持内容类型
                privateMessage.setChannelType(ChatMessage.ChannelType.GROUP); // 类型仍为 GROUP
                privateMessage.setGroupId(groupMessage.getGroupId());

                // 3. 发布
                publisher.publish(privateMessage);
            }
        } catch (Exception e) {
            log.error("处理群聊消息失败: {}", e.getMessage(), e);
            throw new ChatException("GROUP_MESSAGE_ERROR", "群聊消息处理失败: " + e.getMessage(), e);
        }
    }

    private void enrichMessage(ChatMessage message) {
        if (message.getMessageId() == null || message.getMessageId().trim().isEmpty()) {
            message.setMessageId(UUID.randomUUID().toString());
        }

        if (message.getStatus() == null) {
            message.setStatus(ChatMessage.MessageStatus.SENT);
        }

        if (message.getConversationId() == null || message.getConversationId().trim().isEmpty()) {
            if (message.getChannelType() == ChatMessage.ChannelType.PRIVATE) {
                String conversationId = buildPrivateConversationId(message.getSenderId(), message.getReceiverId());
                message.setConversationId(conversationId);
            } else if (message.getChannelType() == ChatMessage.ChannelType.GROUP) {
                if (message.getGroupId() != null) {
                    message.setConversationId("GROUP:" + message.getGroupId());
                }
            }
        }

        if (message.getCreatedAt() == null) {
            message.setCreatedAt(System.currentTimeMillis());
        }
    }

    private String buildPrivateConversationId(String senderId, String receiverId) {
        if (senderId == null || receiverId == null) {
            return null;
        }
        if (senderId.compareTo(receiverId) < 0) {
            return senderId + "-" + receiverId;
        } else {
            return receiverId + "-" + senderId;
        }
    }

    /**
     * 验证消息的有效性
     */
    private void validateMessage(ChatMessage message) {
        if (message == null) {
            throw new ChatException("INVALID_MESSAGE", "消息不能为空");
        }
        
        if (message.getSenderId() == null || message.getSenderId().trim().isEmpty()) {
            throw new ChatException("INVALID_SENDER", "发送者ID不能为空");
        }
        
        if (message.getChannelType() == null) {
            throw new ChatException("INVALID_CHANNEL_TYPE", "信道类型不能为空");
        }
        
        if (message.getContentType() == null) {
            throw new ChatException("INVALID_CONTENT_TYPE", "内容类型不能为空");
        }
        
        if (message.getPayload() == null) {
            throw new ChatException("INVALID_PAYLOAD", "消息载体不能为空");
        }
        
        // 单聊消息必须有接收者ID
        if (message.getChannelType() == ChatMessage.ChannelType.PRIVATE) {
            if (message.getReceiverId() == null || message.getReceiverId().trim().isEmpty()) {
                throw new ChatException("INVALID_RECEIVER", "单聊消息必须指定接收者ID");
            }
        }
        
        // 群聊消息必须有群组ID
        if (message.getChannelType() == ChatMessage.ChannelType.GROUP) {
            if (message.getGroupId() == null || message.getGroupId().trim().isEmpty()) {
                throw new ChatException("INVALID_GROUP_ID", "群聊消息必须指定群组ID");
            }
        }
        
        // 验证载体内容
        validatePayload(message.getContentType(), message.getPayload());
    }

    /**
     * 验证消息载体的有效性
     */
    private void validatePayload(ChatMessage.ContentType contentType, Object payload) {
        switch (contentType) {
            case TEXT:
                // 处理从JSON反序列化的Map对象或TextPayload对象
                String text = null;
                if (payload instanceof ChatMessage.TextPayload) {
                    text = ((ChatMessage.TextPayload) payload).getText();
                } else if (payload instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) payload;
                    Object textObj = map.get("text");
                    text = textObj != null ? textObj.toString() : null;
                } else {
                    throw new ChatException("INVALID_TEXT_PAYLOAD", "文本消息载体类型错误");
                }
                
                if (text == null || text.trim().isEmpty()) {
                    throw new ChatException("EMPTY_TEXT_CONTENT", "文本内容不能为空");
                }
                break;
                
            case IMAGE:
                if (!(payload instanceof ChatMessage.ImagePayload)) {
                    throw new ChatException("INVALID_IMAGE_PAYLOAD", "图片消息载体类型错误");
                }
                ChatMessage.ImagePayload imagePayload = (ChatMessage.ImagePayload) payload;
                if (imagePayload.getUrl() == null || imagePayload.getUrl().trim().isEmpty()) {
                    throw new ChatException("EMPTY_IMAGE_URL", "图片URL不能为空");
                }
                break;
                
            case FILE:
                if (!(payload instanceof ChatMessage.FilePayload)) {
                    throw new ChatException("INVALID_FILE_PAYLOAD", "文件消息载体类型错误");
                }
                ChatMessage.FilePayload filePayload = (ChatMessage.FilePayload) payload;
                if (filePayload.getUrl() == null || filePayload.getUrl().trim().isEmpty()) {
                    throw new ChatException("EMPTY_FILE_URL", "文件URL不能为空");
                }
                break;
                
            case VIDEO:
                if (!(payload instanceof ChatMessage.VideoPayload)) {
                    throw new ChatException("INVALID_VIDEO_PAYLOAD", "视频消息载体类型错误");
                }
                ChatMessage.VideoPayload videoPayload = (ChatMessage.VideoPayload) payload;
                if (videoPayload.getUrl() == null || videoPayload.getUrl().trim().isEmpty()) {
                    throw new ChatException("EMPTY_VIDEO_URL", "视频URL不能为空");
                }
                break;
                
            case AUDIO:
                // 音频载体验证逻辑可以后续添加
                log.warn("音频消息载体验证尚未实现");
                break;
                
            case SYSTEM:
                // 系统消息载体验证逻辑可以后续添加
                log.debug("系统消息载体验证");
                break;
                
            default:
                throw new ChatException("UNSUPPORTED_CONTENT_TYPE", "不支持的内容类型: " + contentType);
        }
    }

    /**
     * 创建文本消息的便捷方法
     */
    public ChatMessage createTextMessage(String senderId, String receiverId, String text) {
        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setChannelType(ChatMessage.ChannelType.PRIVATE);
        message.setContentType(ChatMessage.ContentType.TEXT);
        message.setPayload(new ChatMessage.TextPayload(text));
        return message;
    }

    /**
     * 创建群聊文本消息的便捷方法
     */
    public ChatMessage createGroupTextMessage(String senderId, String groupId, String text) {
        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setGroupId(groupId);
        message.setChannelType(ChatMessage.ChannelType.GROUP);
        message.setContentType(ChatMessage.ContentType.TEXT);
        message.setPayload(new ChatMessage.TextPayload(text));
        return message;
    }

    /**
     * 创建视频消息的便捷方法
     */
    public ChatMessage createVideoMessage(String senderId, String receiverId, String videoUrl, 
                                        String thumbnailUrl, long duration, long sizeInBytes) {
        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setChannelType(ChatMessage.ChannelType.PRIVATE);
        message.setContentType(ChatMessage.ContentType.VIDEO);
        
        ChatMessage.VideoPayload videoPayload = new ChatMessage.VideoPayload();
        videoPayload.setUrl(videoUrl);
        videoPayload.setThumbnailUrl(thumbnailUrl);
        videoPayload.setDuration(duration);
        videoPayload.setSizeInBytes(sizeInBytes);
        message.setPayload(videoPayload);
        
        return message;
    }

    /**
     * 创建群聊视频消息的便捷方法
     */
    public ChatMessage createGroupVideoMessage(String senderId, String groupId, String videoUrl,
                                             String thumbnailUrl, long duration, long sizeInBytes) {
        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setGroupId(groupId);
        message.setChannelType(ChatMessage.ChannelType.GROUP);
        message.setContentType(ChatMessage.ContentType.VIDEO);
        
        ChatMessage.VideoPayload videoPayload = new ChatMessage.VideoPayload();
        videoPayload.setUrl(videoUrl);
        videoPayload.setThumbnailUrl(thumbnailUrl);
        videoPayload.setDuration(duration);
        videoPayload.setSizeInBytes(sizeInBytes);
        message.setPayload(videoPayload);
        
        return message;
    }

    /**
     * 创建图片消息的便捷方法
     */
    public ChatMessage createImageMessage(String senderId, String receiverId, String imageUrl,
                                        String filename, long sizeInBytes) {
        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setChannelType(ChatMessage.ChannelType.PRIVATE);
        message.setContentType(ChatMessage.ContentType.IMAGE);
        
        ChatMessage.ImagePayload imagePayload = new ChatMessage.ImagePayload();
        imagePayload.setUrl(imageUrl);
        imagePayload.setFilename(filename);
        imagePayload.setSizeInBytes(sizeInBytes);
        message.setPayload(imagePayload);
        
        return message;
    }

    /**
     * 创建文件消息的便捷方法
     */
    public ChatMessage createFileMessage(String senderId, String receiverId, String fileUrl,
                                       String fileName, String fileExtension, long sizeInBytes) {
        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setChannelType(ChatMessage.ChannelType.PRIVATE);
        message.setContentType(ChatMessage.ContentType.FILE);
        
        ChatMessage.FilePayload filePayload = new ChatMessage.FilePayload();
        filePayload.setUrl(fileUrl);
        filePayload.setFileName(fileName);
        filePayload.setFileExtension(fileExtension);
        filePayload.setSizeInBytes(sizeInBytes);
        message.setPayload(filePayload);
        
        return message;
    }

    // ==================== 新增功能方法 ====================

    /**
     * 用户本地删除消息
     *
     * <p>在 deleted_messages 集合中记录用户删除的消息，
     * 该用户在查看历史时不会看到这些消息。</p>
     *
     * @param userId 执行删除操作的用户ID
     * @param messageIds 要删除的消息ID列表
     * @return 删除的消息数量
     */
    public int deleteMessagesForUser(String userId, List<String> messageIds) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new ChatException("INVALID_USER_ID", "userId 不能为空");
        }
        if (messageIds == null || messageIds.isEmpty()) {
            return 0;
        }

        long now = System.currentTimeMillis();
        int deletedCount = 0;

        for (String messageId : messageIds) {
            // 检查是否已经删除过
            if (!deletedMessageMapper.existsByUserIdAndMessageId(userId, messageId)) {
                // 获取消息的会话ID
                ChatMessageDocument messageDoc = chatMessageMapper.findByMessageId(messageId).orElse(null);
                if (messageDoc != null) {
                    DeletedMessageDocument deletedDoc = new DeletedMessageDocument();
                    deletedDoc.setUserId(userId);
                    deletedDoc.setMessageId(messageId);
                    deletedDoc.setConversationId(messageDoc.getConversationId());
                    deletedDoc.setDeletedAt(now);
                    deletedMessageMapper.save(deletedDoc);
                    deletedCount++;
                }
            }
        }
        return deletedCount;
    }

    /**
     * 获取用户在某会话中删除的消息ID列表（内部方法）
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return 已删除的消息ID列表
     */
    public List<String> getDeletedMessageIds(String userId, String conversationId) {
        if (userId == null || conversationId == null) {
            return Collections.emptyList();
        }
        return deletedMessageMapper.findByUserIdAndConversationId(userId, conversationId)
                .stream()
                .map(DeletedMessageDocument::getMessageId)
                .collect(Collectors.toList());
    }

    /**
     * 获取带删除过滤的历史消息
     *
     * <p>在原有 getHistory 基础上，过滤掉用户已删除的消息。</p>
     *
     * @param conversationId 会话ID
     * @param userId 用户ID（可选，为空则不过滤删除消息）
     * @param cursor 游标
     * @param size 每页数量
     * @return 过滤后的消息列表
     */
    public List<ChatMessage> getHistoryWithDeletionFilter(String conversationId, String userId, Long cursor, int size) {
        List<ChatMessage> messages = getHistory(conversationId, cursor, size);
        
        if (userId == null || userId.trim().isEmpty()) {
            return messages;
        }

        // 获取用户删除的消息ID
        List<String> deletedMessageIds = getDeletedMessageIds(userId, conversationId);
        if (deletedMessageIds.isEmpty()) {
            return messages;
        }

        // 过滤掉已删除的消息
        return messages.stream()
                .filter(msg -> !deletedMessageIds.contains(msg.getMessageId()))
                .collect(Collectors.toList());
    }

    /**
     * 添加消息收藏
     *
     * @param userId 用户ID
     * @param messageId 消息ID
     * @return 是否成功添加（false表示已经收藏过）
     */
    public boolean addFavorite(String userId, String messageId) {
        if (userId == null || messageId == null) {
            throw new ChatException("INVALID_PARAMS", "userId 和 messageId 不能为空");
        }

        // 检查是否已收藏
        if (favoriteMessageMapper.existsByUserIdAndMessageId(userId, messageId)) {
            return false;
        }

        // 获取消息信息
        ChatMessageDocument messageDoc = chatMessageMapper.findByMessageId(messageId)
                .orElseThrow(() -> new ChatException("MESSAGE_NOT_FOUND", "消息不存在"));

        FavoriteMessageDocument favoriteDoc = new FavoriteMessageDocument();
        favoriteDoc.setUserId(userId);
        favoriteDoc.setMessageId(messageId);
        favoriteDoc.setConversationId(messageDoc.getConversationId());
        favoriteDoc.setCreatedAt(System.currentTimeMillis());
        favoriteMessageMapper.save(favoriteDoc);
        return true;
    }

    /**
     * 移除消息收藏
     *
     * @param userId 用户ID
     * @param messageId 消息ID
     * @return 是否成功移除（false表示本来就没有收藏）
     */
    public boolean removeFavorite(String userId, String messageId) {
        if (userId == null || messageId == null) {
            throw new ChatException("INVALID_PARAMS", "userId 和 messageId 不能为空");
        }

        if (!favoriteMessageMapper.existsByUserIdAndMessageId(userId, messageId)) {
            return false;
        }

        favoriteMessageMapper.deleteByUserIdAndMessageId(userId, messageId);
        return true;
    }

    /**
     * 获取用户收藏的消息列表
     *
     * @param userId 用户ID
     * @param conversationId 会话ID（可选）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 收藏消息分页结果
     */
    public Map<String, Object> listFavorites(String userId, String conversationId, int page, int size) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new ChatException("INVALID_USER_ID", "userId 不能为空");
        }
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<FavoriteMessageDocument> favoritePage;
        
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            favoritePage = favoriteMessageMapper.findByUserIdAndConversationIdOrderByCreatedAtDesc(userId, conversationId, pageable);
        } else {
            favoritePage = favoriteMessageMapper.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        // 获取消息ID列表
        List<String> messageIds = favoritePage.getContent().stream()
                .map(FavoriteMessageDocument::getMessageId)
                .collect(Collectors.toList());

        // 批量查询消息详情
        List<ChatMessage> messages = Collections.emptyList();
        if (!messageIds.isEmpty()) {
            List<ChatMessageDocument> messageDocs = chatMessageMapper.findByMessageIdIn(messageIds);
            messages = messageDocs.stream().map(this::fromDocument).collect(Collectors.toList());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", favoritePage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("messages", messages);
        return result;
    }

    /**
     * 置顶消息
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @return 是否成功置顶（false表示已经置顶过）
     */
    public boolean pinMessage(String userId, String conversationId, String messageId) {
        if (userId == null || conversationId == null || messageId == null) {
            throw new ChatException("INVALID_PARAMS", "userId、conversationId 和 messageId 不能为空");
        }

        // 检查是否已置顶
        if (pinnedMessageMapper.existsByUserIdAndConversationIdAndMessageId(userId, conversationId, messageId)) {
            return false;
        }

        // 验证消息存在
        if (!chatMessageMapper.findByMessageId(messageId).isPresent()) {
            throw new ChatException("MESSAGE_NOT_FOUND", "消息不存在");
        }

        PinnedMessageDocument pinnedDoc = new PinnedMessageDocument();
        pinnedDoc.setUserId(userId);
        pinnedDoc.setConversationId(conversationId);
        pinnedDoc.setMessageId(messageId);
        pinnedDoc.setPinnedAt(System.currentTimeMillis());
        pinnedMessageMapper.save(pinnedDoc);
        return true;
    }

    /**
     * 取消置顶消息
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @return 是否成功取消置顶（false表示本来就没有置顶）
     */
    public boolean unpinMessage(String userId, String conversationId, String messageId) {
        if (userId == null || conversationId == null || messageId == null) {
            throw new ChatException("INVALID_PARAMS", "userId、conversationId 和 messageId 不能为空");
        }

        if (!pinnedMessageMapper.existsByUserIdAndConversationIdAndMessageId(userId, conversationId, messageId)) {
            return false;
        }

        pinnedMessageMapper.deleteByUserIdAndConversationIdAndMessageId(userId, conversationId, messageId);
        return true;
    }

    /**
     * 获取用户在某会话中的置顶消息列表
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return 置顶消息列表
     */
    public List<ChatMessage> listPinnedMessages(String userId, String conversationId) {
        if (userId == null || conversationId == null) {
            throw new ChatException("INVALID_PARAMS", "userId 和 conversationId 不能为空");
        }

        List<PinnedMessageDocument> pinnedDocs = pinnedMessageMapper
                .findByUserIdAndConversationIdOrderByPinnedAtDesc(userId, conversationId);
        
        if (pinnedDocs.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> messageIds = pinnedDocs.stream()
                .map(PinnedMessageDocument::getMessageId)
                .collect(Collectors.toList());

        List<ChatMessageDocument> messageDocs = chatMessageMapper.findByMessageIdIn(messageIds);
        return messageDocs.stream().map(this::fromDocument).collect(Collectors.toList());
    }

    /**
     * 记录消息已读回执
     *
     * @param messageId 消息ID
     * @param userId 已读用户ID
     * @return 是否成功记录（false表示已经记录过）
     */
    public boolean recordReadReceipt(String messageId, String userId) {
        if (messageId == null || userId == null) {
            throw new ChatException("INVALID_PARAMS", "messageId 和 userId 不能为空");
        }

        // 检查是否已记录
        if (messageReadReceiptMapper.existsByMessageIdAndUserId(messageId, userId)) {
            return false;
        }

        // 获取消息信息
        ChatMessageDocument messageDoc = chatMessageMapper.findByMessageId(messageId)
                .orElseThrow(() -> new ChatException("MESSAGE_NOT_FOUND", "消息不存在"));

        MessageReadReceiptDocument receiptDoc = new MessageReadReceiptDocument();
        receiptDoc.setMessageId(messageId);
        receiptDoc.setUserId(userId);
        receiptDoc.setConversationId(messageDoc.getConversationId());
        receiptDoc.setGroupId(messageDoc.getGroupId());
        receiptDoc.setReadAt(System.currentTimeMillis());
        messageReadReceiptMapper.save(receiptDoc);
        return true;
    }

    /**
     * 获取消息的已读回执详情
     *
     * @param messageId 消息ID
     * @return 包含 readCount 和 receipts 的 Map
     */
    public Map<String, Object> getReadReceipts(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new ChatException("INVALID_MESSAGE_ID", "messageId 不能为空");
        }

        List<MessageReadReceiptDocument> receipts = messageReadReceiptMapper.findByMessageId(messageId);
        long readCount = messageReadReceiptMapper.countByMessageId(messageId);

        List<Map<String, Object>> receiptList = receipts.stream()
                .map(receipt -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("userId", receipt.getUserId());
                    item.put("readAt", receipt.getReadAt());
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("readCount", readCount);
        result.put("receipts", receiptList);
        return result;
    }

    /**
     * 发送正在输入信令
     *
     * <p>构造 SYSTEM 类型的 ChatMessage 并通过 Redis 发布，不持久化到数据库。</p>
     *
     * @param fromUserId 发送输入状态的用户ID
     * @param toUserId 接收输入状态的用户ID（单聊）
     * @param conversationId 会话ID
     * @param typing 是否正在输入
     */
    public void sendTypingIndicator(String fromUserId, String toUserId, String conversationId, boolean typing) {
        if (fromUserId == null || toUserId == null || conversationId == null) {
            throw new ChatException("INVALID_PARAMS", "fromUserId、toUserId 和 conversationId 不能为空");
        }

        ChatMessage typingMessage = new ChatMessage();
        typingMessage.setMessageId(UUID.randomUUID().toString());
        typingMessage.setSenderId(fromUserId);
        typingMessage.setReceiverId(toUserId);
        typingMessage.setConversationId(conversationId);
        typingMessage.setChannelType(ChatMessage.ChannelType.PRIVATE);
        typingMessage.setContentType(ChatMessage.ContentType.SYSTEM);
        typingMessage.setCreatedAt(System.currentTimeMillis());

        // 构造 typing 载体
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "TYPING");
        payload.put("conversationId", conversationId);
        payload.put("typing", typing);
        typingMessage.setPayload(payload);

        // 只发布到 Redis，不持久化
        publisher.publish(typingMessage);
        log.debug("发送 typing 信令: {} -> {}, typing: {}", fromUserId, toUserId, typing);
    }

    /**
     * 简化版全局消息搜索
     *
     * <p>跨会话搜索用户相关的消息，仅对文本消息进行关键字匹配。
     * 后续可替换为 Elasticsearch 实现。</p>
     *
     * @param userId 用户ID（作为发送者或接收者）
     * @param keyword 关键字
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 搜索结果 Map
     */
    public Map<String, Object> searchGlobalMessages(String userId, String keyword, int page, int size) {
        //TODO 全局搜索需要移动到IM-search服务中
        return Collections.emptyMap();
    }
}