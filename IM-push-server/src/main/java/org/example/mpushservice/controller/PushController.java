package org.example.mpushservice.controller;

import com.example.domain.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mpushservice.service.PushService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 推送服务控制器
 * @deprecated 已改用 RabbitMQ 进行消息推送，此 Controller 已废弃
 * 保留仅用于测试或临时兼容
 */
@Deprecated
@Slf4j
@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PushController {

    private final PushService pushService;

    /**
     * 推送系统事件（已废弃）
     * @deprecated 系统事件现在通过 RabbitMQ 自动推送
     * @param event 系统事件消息
     * @return 推送结果
     */
    @Deprecated
    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> pushEvent(@RequestBody ChatMessage event) {
        log.warn("使用了已废弃的 HTTP 推送接口，建议改用 RabbitMQ");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "此接口已废弃，请使用 RabbitMQ 进行消息推送");
        return ResponseEntity.status(410).body(response); // 410 Gone
    }
}
