package org.example.imserver.service;

import com.example.domain.dto.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.example.imserver.exception.ChatException;
import org.springframework.stereotype.Service;
import org.example.imserver.websocket.RedisMessagePublisher;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ChatService {

    private final RedisMessagePublisher publisher;

    public ChatService(RedisMessagePublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * 主入口：处理来自 WebSocket 的消息
     */
    public void handleMessage(ChatMessage chatMessage) {
        try {
            // 验证消息
            validateMessage(chatMessage);
            
            if (chatMessage.getChannelType() == ChatMessage.ChannelType.PRIVATE) {
                // 2. 单聊：直接发布
                // Redis 订阅者会处理投递
                publisher.publish(chatMessage);
                log.debug("单聊消息发送 {} to {}",
                    chatMessage.getSenderId(), chatMessage.getReceiverId());

            } else if (chatMessage.getChannelType() == ChatMessage.ChannelType.GROUP) {
                // 3. 群聊：执行 "Fan-out"（扇出）
                handleGroupMessage(chatMessage);
            }
        } catch (Exception e) {
            log.error("处理消息失败: {}", e.getMessage(), e);
            throw new ChatException("MESSAGE_HANDLE_ERROR", "消息处理失败: " + e.getMessage(), e);
        }
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
}