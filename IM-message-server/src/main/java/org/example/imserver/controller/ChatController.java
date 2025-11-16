package org.example.imserver.controller;

import com.example.domain.dto.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.example.imserver.service.ChatService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 聊天 REST API 控制器
 * @author Junjie
 * @date 2025-11-13
 */
@Slf4j
@RestController
@RequestMapping("/v1/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String ONLINE_USERS_KEY = "global:online:users";

    public ChatController(ChatService chatService, StringRedisTemplate stringRedisTemplate) {
        this.chatService = chatService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody ChatMessage message) {
        try {
            chatService.handleMessage(message);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "消息发送成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "消息发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "IM-message-server");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取在线用户数量
     */
    @GetMapping("/online-users")
    public ResponseEntity<Map<String, Object>> getOnlineUsers() {
        // 这里可以实现获取在线用户的逻辑
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", stringRedisTemplate.opsForSet().size(ONLINE_USERS_KEY)); // 暂时返回0，可以后续实现
        response.put("message", "OK");
        return ResponseEntity.ok(response);
    }
}
