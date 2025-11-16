package org.example.imserver.controller;

import com.example.domain.dto.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.example.imserver.service.ChatService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/api/groupChat")
@CrossOrigin(origins = "*")
public class ChatGroupController {

    private final ChatService chatService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String ONLINE_USERS_KEY = "global:online:users";

    public ChatGroupController(ChatService chatService, StringRedisTemplate stringRedisTemplate) {
        this.chatService = chatService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 发送群聊文本消息
     */
    @PostMapping("/send-group-text")
    public ResponseEntity<Map<String, Object>> sendGroupTextMessage(
            @RequestParam String senderId,
            @RequestParam String groupId,
            @RequestParam String text) {
        try {
            ChatMessage message = chatService.createGroupTextMessage(senderId, groupId, text);
            chatService.handleMessage(message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "群聊文本消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送群聊文本消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送群聊图片消息
     */
    @PostMapping("/send-group-image")
    public ResponseEntity<Map<String, Object>> sendGroupImageMessage(
            @RequestParam String senderId,
            @RequestParam String groupId,
            @RequestParam String imageUrl,
            @RequestParam String filename,
            @RequestParam(defaultValue = "0") long sizeInBytes) {
        try {
            ChatMessage message = chatService.createImageMessage(senderId, null, imageUrl, filename, sizeInBytes);
            message.setGroupId(groupId);
            message.setChannelType(ChatMessage.ChannelType.GROUP);
            message.setReceiverId(null); // 群聊不需要接收者ID

            chatService.handleMessage(message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "群聊图片消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送群聊图片消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送群聊文件消息
     */
    @PostMapping("/send-group-file")
    public ResponseEntity<Map<String, Object>> sendGroupFileMessage(
            @RequestParam String senderId,
            @RequestParam String groupId,
            @RequestParam String fileUrl,
            @RequestParam String fileName,
            @RequestParam String fileExtension,
            @RequestParam(defaultValue = "0") long sizeInBytes) {
        try {
            ChatMessage message = chatService.createFileMessage(senderId, null, fileUrl, fileName, fileExtension, sizeInBytes);
            message.setGroupId(groupId);
            message.setChannelType(ChatMessage.ChannelType.GROUP);
            message.setReceiverId(null); // 群聊不需要接收者ID

            chatService.handleMessage(message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "群聊文件消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送群聊文件消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送群聊视频消息
     */
    @PostMapping("/send-group-video")
    public ResponseEntity<Map<String, Object>> sendGroupVideoMessage(
            @RequestParam String senderId,
            @RequestParam String groupId,
            @RequestParam String videoUrl,
            @RequestParam(required = false) String thumbnailUrl,
            @RequestParam(defaultValue = "0") long duration,
            @RequestParam(defaultValue = "0") long sizeInBytes) {
        try {
            ChatMessage message = chatService.createGroupVideoMessage(senderId, groupId, videoUrl, thumbnailUrl, duration, sizeInBytes);
            chatService.handleMessage(message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "群聊视频消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送群聊视频消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送群聊音频消息
     */
    @PostMapping("/send-group-audio")
    public ResponseEntity<Map<String, Object>> sendGroupAudioMessage(
            @RequestParam String senderId,
            @RequestParam String groupId,
            @RequestParam String audioUrl,
            @RequestParam(defaultValue = "0") long duration,
            @RequestParam(defaultValue = "0") long sizeInBytes) {
        try {
            ChatMessage message = new ChatMessage();
            message.setSenderId(senderId);
            message.setGroupId(groupId);
            message.setChannelType(ChatMessage.ChannelType.GROUP);
            message.setContentType(ChatMessage.ContentType.AUDIO);

            // 创建音频载体
            Map<String, Object> audioPayload = new HashMap<>();
            audioPayload.put("url", audioUrl);
            audioPayload.put("duration", duration);
            audioPayload.put("sizeInBytes", sizeInBytes);
            message.setPayload(audioPayload);

            chatService.handleMessage(message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "群聊音频消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送群聊音频消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    /**
     * 发送群聊系统消息
     */
    @PostMapping("/send-group-system")
    public ResponseEntity<Map<String, Object>> sendGroupSystemMessage(
            @RequestParam String groupId,
            @RequestParam String content) {
        try {
            ChatMessage message = new ChatMessage();
            message.setSenderId("SYSTEM");
            message.setGroupId(groupId);
            message.setChannelType(ChatMessage.ChannelType.GROUP);
            message.setContentType(ChatMessage.ContentType.SYSTEM);

            // 创建系统消息载体
            Map<String, Object> systemPayload = new HashMap<>();
            systemPayload.put("text", content);
            systemPayload.put("timestamp", System.currentTimeMillis());
            message.setPayload(systemPayload);

            chatService.handleMessage(message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "群聊系统消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送群聊系统消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
