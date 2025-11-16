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
@RequestMapping("/v1/api/privateChat")
@CrossOrigin(origins = "*")
public class ChatPrivateController {
    private final ChatService chatService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String ONLINE_USERS_KEY = "global:online:users";

    public ChatPrivateController(ChatService chatService, StringRedisTemplate stringRedisTemplate) {
        this.chatService = chatService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostMapping("/send-text")
    public ResponseEntity<Map<String, Object>> sendTextMessage(
            @RequestParam String senderId,
            @RequestParam String receiverId,
            @RequestParam String text) {
        try {
            ChatMessage message = chatService.createTextMessage(senderId, receiverId, text);
            chatService.handleMessage(message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文本消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送文本消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送单聊图片消息
     */
    @PostMapping("/send-image")
    public ResponseEntity<Map<String, Object>> sendImageMessage(
            @RequestParam String senderId,
            @RequestParam String receiverId,
            @RequestParam String imageUrl,
            @RequestParam String filename,
            @RequestParam(defaultValue = "0") long sizeInBytes) {
        try {
            ChatMessage message = chatService.createImageMessage(senderId, receiverId, imageUrl, filename, sizeInBytes);
            chatService.handleMessage(message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "图片消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送图片消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送单聊文件消息
     */
    @PostMapping("/send-file")
    public ResponseEntity<Map<String, Object>> sendFileMessage(
            @RequestParam String senderId,
            @RequestParam String receiverId,
            @RequestParam String fileUrl,
            @RequestParam String fileName,
            @RequestParam String fileExtension,
            @RequestParam(defaultValue = "0") long sizeInBytes) {
        try {
            ChatMessage message = chatService.createFileMessage(senderId, receiverId, fileUrl, fileName, fileExtension, sizeInBytes);
            chatService.handleMessage(message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文件消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送文件消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送单聊视频消息
     */
    @PostMapping("/send-video")
    public ResponseEntity<Map<String, Object>> sendVideoMessage(
            @RequestParam String senderId,
            @RequestParam String receiverId,
            @RequestParam String videoUrl,
            @RequestParam(required = false) String thumbnailUrl,
            @RequestParam(defaultValue = "0") long duration,
            @RequestParam(defaultValue = "0") long sizeInBytes) {
        try {
            ChatMessage message = chatService.createVideoMessage(senderId, receiverId, videoUrl, thumbnailUrl, duration, sizeInBytes);
            chatService.handleMessage(message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "视频消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送视频消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送单聊音频消息
     */
    @PostMapping("/send-audio")
    public ResponseEntity<Map<String, Object>> sendAudioMessage(
            @RequestParam String senderId,
            @RequestParam String receiverId,
            @RequestParam String audioUrl,
            @RequestParam(defaultValue = "0") long duration,
            @RequestParam(defaultValue = "0") long sizeInBytes) {
        try {
            ChatMessage message = new ChatMessage();
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.setChannelType(ChatMessage.ChannelType.PRIVATE);
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
            response.put("message", "音频消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送音频消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    public StringRedisTemplate getStringRedisTemplate() {
        return stringRedisTemplate;
    }
}
