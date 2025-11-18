package org.example.imserver.controller;

import com.example.domain.dto.ChatMessage;
import com.example.domain.dto.ForwardMessageRequest;
import com.example.domain.dto.DeleteMessageRequest;
import com.example.domain.dto.FavoriteMessageRequest;
import com.example.domain.dto.PinMessageRequest;
import com.example.domain.dto.TypingIndicatorRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.imserver.service.ChatService;
import org.example.imserver.websocket.WebSocketSessionManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天 REST API 控制器
 *
 * <p>提供基于 HTTP 的即时消息发送、历史查询、状态更新、撤回、批量已读、未读统计等接口，
 * 主要面向 Web / 移动端 REST 调用。</p>
 *
 * @author Junjie
 * @date 2025-11-13
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final StringRedisTemplate stringRedisTemplate;
    private final WebSocketSessionManager sessionManager;

    private static final String ONLINE_USERS_KEY = "global:online:users";

    public ChatController(ChatService chatService, StringRedisTemplate stringRedisTemplate, WebSocketSessionManager sessionManager) {
        this.chatService = chatService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sessionManager = sessionManager;
    }

    /**
     * 发送消息
     *
     * <p>请求体为 ChatMessage，服务端会补全 messageId / conversationId / status / createdAt 等字段，
     * 写入 MongoDB 并通过 Redis 发布给在线终端。</p>
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody ChatMessage message) {
        try {
            chatService.handleMessage(message);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "消息发送成功");
            response.put("data", message);
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
     * 获取会话历史消息
     *
     * <p>基于 conversationId（单聊：userA-userB；群聊：GROUP:groupId）和 createdAt 游标分页，
     * 返回最近一批消息和 nextCursor，前端可据此上拉加载更多历史记录。</p>
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(@RequestParam String conversationId,
                                                          @RequestParam(required = false) Long cursor,
                                                          @RequestParam(required = false, defaultValue = "50") Integer size) {
        try {
            List<ChatMessage> messages = chatService.getHistory(conversationId, cursor, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "\u83b7\u53d6\u5386\u53f2\u6d88\u606f\u6210\u529f");
            response.put("data", messages);
            Long nextCursor = null;
            if (messages != null && !messages.isEmpty()) {
                ChatMessage last = messages.get(messages.size() - 1);
                nextCursor = last.getCreatedAt();
            }
            response.put("nextCursor", nextCursor);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取历史消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "\u83b7\u53d6\u5386\u53f2\u6d88\u606f\u5931\u8d25: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 消息送达确认
     *
     * <p>将指定消息的状态更新为 DELIVERED，用于“已送达”回执场景。</p>
     */
    @PostMapping("/ack-delivered")
    public ResponseEntity<Map<String, Object>> ackDelivered(@RequestParam String messageId) {
        try {
            ChatMessage updated = chatService.updateStatus(messageId, ChatMessage.MessageStatus.DELIVERED);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "\u66f4\u65b0\u6d88\u606f\u72b6\u6001\u4e3a DELIVERED \u6210\u529f");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("\u66f4\u65b0\u6d88\u606f\u72b6\u6001 DELIVERED \u5931\u8d25: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "\u66f4\u65b0\u6d88\u606f\u72b6\u6001\u5931\u8d25: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 消息已读确认
     *
     * 将指定消息的状态更新为 READ，通常由接收端在消息展示后调用。
     * 如果提供 userId 参数，还会记录群聊已读回执.
     */
    @PostMapping("/ack-read")
    public ResponseEntity<Map<String, Object>> ackRead(@RequestParam String messageId,
                                                       @RequestParam(required = false) String userId) {
        try {
            ChatMessage updated = chatService.updateStatus(messageId, ChatMessage.MessageStatus.READ);
            
            // 如果提供了 userId，记录已读回执（用于群聊已读人数统计）
            if (userId != null && !userId.trim().isEmpty()) {
                chatService.recordReadReceipt(messageId, userId);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "更新消息状态为 READ 成功");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("更新消息状态 read 失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新消息状态失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 撤回消息
     *
     * <p>默认仅允许在 2 分钟内撤回指定 messageId 对应的消息，超时会返回业务错误。</p>
     */
    @PostMapping("/recall")
    public ResponseEntity<Map<String, Object>> recallMessage(@RequestParam String messageId) {
        try {
            ChatMessage updated = chatService.recallMessage(messageId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "消息撤回成功");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("撤回消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "消息撤回失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 批量标记消息为已读
     *
     * <p>请求体中携带 messageIds 数组，服务端会将这些消息的状态更新为 READ。</p>
     */
    @PostMapping("/read-batch")
    public ResponseEntity<Map<String, Object>> readBatch(@RequestBody Map<String, List<String>> body) {
        try {
            List<String> messageIds = body != null ? body.get("messageIds") : null;
            List<ChatMessage> updated = chatService.markMessagesRead(messageIds);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量标记已读成功");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("批量标记已读失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量标记已读失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取用户未读消息汇总
     *
     * <p>按会话维度聚合指定 userId 的未读消息数量，并返回每个会话的最后一条未读消息，
     * 适合用于会话列表未读红点和摘要展示。</p>
     */
    @GetMapping("/unread/summary")
    public ResponseEntity<Map<String, Object>> getUnreadSummary(@RequestParam String userId) {
        try {
            Map<String, Object> data = chatService.getUnreadSummary(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取未读消息汇总成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取未读消息汇总失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取未读消息汇总失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取在线用户数
     *
     * <p>返回当前在线用户数，优先使用 WebSocket 会话管理器获取，如果不可用则从 Redis 中获取。</p>
     */
    @GetMapping("/online-users")
    public ResponseEntity<Map<String, Object>> getOnlineUsers() {
        try {
            // 优先使用WebSocket会话管理器获取在线用户数
            int wsCount = 0;
            try {
                wsCount = sessionManager.getOnlineUserCount();
                log.debug("从WebSocket获取在线用户数: {}", wsCount);
            } catch (Exception e) {
                log.debug("无法从WebSocket获取在线用户数: {}", e.getMessage());
            }
            
            // 尝试从Redis获取
            Long redisCount = 0L;
            try {
                redisCount = stringRedisTemplate.opsForSet().size(ONLINE_USERS_KEY);
                log.debug("从Redis获取在线用户数: {}", redisCount);
            } catch (Exception e) {
                log.debug("无法从Redis获取在线用户数: {}", e.getMessage());
            }
            
            // 使用WebSocket的数据作为主要数据源
            int finalCount = Math.max(wsCount, redisCount != null ? redisCount.intValue() : 0);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", finalCount);
            response.put("wsCount", wsCount);
            response.put("redisCount", redisCount);
            response.put("onlineUsers", sessionManager.getOnlineUsers()); // 返回在线用户列表
            response.put("message", "获取在线用户数成功");
            
            log.info("返回在线用户数: WebSocket={}, Redis={}, 最终={}", wsCount, redisCount, finalCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取在线用户数失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("count", 0);
            response.put("message", "获取在线用户数失败: " + e.getMessage());
            return ResponseEntity.ok(response); // 改为200状态码，避免前端报错
        }
    }

    /**
     * 转发消息
     *
     * <p>基于已有消息ID，将其内容转发到多个目标用户或群组，
     * 服务端会为每个目标构造新的消息并复用原始载体。</p>
     */
    @PostMapping("/forward")
    public ResponseEntity<Map<String, Object>> forwardMessage(@RequestBody ForwardMessageRequest request) {
        try {
            Map<String, Object> data = chatService.forwardMessage(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "消息转发成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("消息转发失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "消息转发失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 简单消息搜索
     *
     * <p>在指定会话内按发送者、内容类型、关键字进行过滤搜索，
     * 关键字仅对文本消息的文本内容进行包含匹配。</p>
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchMessages(@RequestParam String conversationId,
                                                              @RequestParam(required = false) String senderId,
                                                              @RequestParam(required = false) String contentType,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "50") int size) {
        try {
            ChatMessage.ContentType ct = null;
            if (contentType != null && !contentType.trim().isEmpty()) {
                ct = ChatMessage.ContentType.valueOf(contentType.toUpperCase());
            }
            Map<String, Object> data = chatService.searchMessages(conversationId, senderId, ct, keyword, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "搜索消息成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("搜索消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "搜索消息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 消息删除（用户本地）
     *
     * <p>用户本地删除消息，不影响其他用户，在该用户查看历史时不会显示。</p>
     */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteMessages(@RequestBody DeleteMessageRequest request) {
        try {
            int deletedCount = chatService.deleteMessagesForUser(request.getUserId(), request.getMessageIds());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "消息删除成功");
            Map<String, Object> data = new HashMap<>();
            data.put("deletedCount", deletedCount);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("消息删除失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "消息删除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取带删除过滤的历史消息
     *
     * <p>在原有 /history 基础上，过滤掉用户已删除的消息。</p>
     */
    @GetMapping("/history/filtered")
    public ResponseEntity<Map<String, Object>> getHistoryFiltered(@RequestParam String conversationId,
                                                                  @RequestParam String userId,
                                                                  @RequestParam(required = false) Long cursor,
                                                                  @RequestParam(required = false, defaultValue = "50") Integer size) {
        try {
            List<ChatMessage> messages = chatService.getHistoryWithDeletionFilter(conversationId, userId, cursor, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取历史消息成功");
            response.put("data", messages);
            Long nextCursor = null;
            if (messages != null && !messages.isEmpty()) {
                ChatMessage last = messages.get(messages.size() - 1);
                nextCursor = last.getCreatedAt();
            }
            response.put("nextCursor", nextCursor);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取历史消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取历史消息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 消息收藏操作
     *
     * <p>添加或移除消息收藏，根据 action 参数决定操作类型。</p>
     */
    @PostMapping("/favorite")
    public ResponseEntity<Map<String, Object>> favoriteMessage(@RequestBody FavoriteMessageRequest request) {
        try {
            boolean success;
            String message;
            if ("ADD".equalsIgnoreCase(request.getAction())) {
                success = chatService.addFavorite(request.getUserId(), request.getMessageId());
                message = success ? "收藏成功" : "消息已经收藏过";
            } else if ("REMOVE".equalsIgnoreCase(request.getAction())) {
                success = chatService.removeFavorite(request.getUserId(), request.getMessageId());
                message = success ? "取消收藏成功" : "消息本来就没有收藏";
            } else {
                throw new IllegalArgumentException("action 只能为 ADD 或 REMOVE");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("消息收藏操作失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "消息收藏操作失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取收藏消息列表
     *
     * <p>分页获取用户收藏的消息，可按会话过滤。</p>
     */
    @GetMapping("/favorites")
    public ResponseEntity<Map<String, Object>> getFavorites(@RequestParam String userId,
                                                            @RequestParam(required = false) String conversationId,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        try {
            Map<String, Object> data = chatService.listFavorites(userId, conversationId, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取收藏消息成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取收藏消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取收藏消息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 消息置顶操作
     *
     * <p>置顶或取消置顶消息，根据 action 参数决定操作类型。</p>
     */
    @PostMapping("/pin")
    public ResponseEntity<Map<String, Object>> pinMessage(@RequestBody PinMessageRequest request) {
        try {
            boolean success;
            String message;
            if ("PIN".equalsIgnoreCase(request.getAction())) {
                success = chatService.pinMessage(request.getUserId(), request.getConversationId(), request.getMessageId());
                message = success ? "置顶成功" : "消息已经置顶过";
            } else if ("UNPIN".equalsIgnoreCase(request.getAction())) {
                success = chatService.unpinMessage(request.getUserId(), request.getConversationId(), request.getMessageId());
                message = success ? "取消置顶成功" : "消息本来就没有置顶";
            } else {
                throw new IllegalArgumentException("action 只能为 PIN 或 UNPIN");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("消息置顶操作失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "消息置顶操作失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取置顶消息列表
     *
     * <p>获取用户在某会话中的置顶消息列表。</p>
     */
    @GetMapping("/pins")
    public ResponseEntity<Map<String, Object>> getPinnedMessages(@RequestParam String userId,
                                                                @RequestParam String conversationId) {
        try {
            List<ChatMessage> messages = chatService.listPinnedMessages(userId, conversationId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取置顶消息成功");
            Map<String, Object> data = new HashMap<>();
            data.put("messages", messages);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取置顶消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取置顶消息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取消息已读回执详情
     *
     * <p>获取某条消息的已读人数和详细回执信息，主要用于群聊。</p>
     */
    @GetMapping("/read/detail")
    public ResponseEntity<Map<String, Object>> getReadDetail(@RequestParam String messageId) {
        try {
            Map<String, Object> data = chatService.getReadReceipts(messageId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取已读详情成功");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取已读详情失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取已读详情失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送正在输入信令
     *
     * <p>通过 Redis 发送正在输入状态，不持久化到数据库。</p>
     */
    @PostMapping("/typing")
    public ResponseEntity<Map<String, Object>> sendTyping(@RequestBody TypingIndicatorRequest request) {
        try {
            boolean typing = Boolean.TRUE.equals(request.getTyping());
            chatService.sendTypingIndicator(request.getFromUserId(), request.getToUserId(),
                                          request.getConversationId(), typing);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "发送 typing 信令成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送 typing 信令失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送 typing 信令失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 全局消息搜索
     *
     * <p>跨会话搜索用户相关的消息，仅对文本消息进行关键字匹配。</p>
     */
    @GetMapping("/search/global")
    public ResponseEntity<Map<String, Object>> searchGlobal(@RequestParam String userId,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "50") int size) {
        //TODO 全局搜索需要移动到IM-search服务中
        return null;
    }

    // ==================== 单聊专用接口 ====================

    /**
     * 发送单聊文本消息
     */
    @PostMapping("/private/send-text")
    public ResponseEntity<Map<String, Object>> sendPrivateTextMessage(
            @RequestParam String senderId,
            @RequestParam String receiverId,
            @RequestParam String text) {
        try {
            ChatMessage message = chatService.createTextMessage(senderId, receiverId, text);
            chatService.handleMessage(message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文本消息发送成功");
            response.put("data", message);
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
    @PostMapping("/private/send-image")
    public ResponseEntity<Map<String, Object>> sendPrivateImageMessage(
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
            response.put("data", message);
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
    @PostMapping("/private/send-file")
    public ResponseEntity<Map<String, Object>> sendPrivateFileMessage(
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
            response.put("data", message);
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
    @PostMapping("/private/send-video")
    public ResponseEntity<Map<String, Object>> sendPrivateVideoMessage(
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
            response.put("data", message);
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
    @PostMapping("/private/send-audio")
    public ResponseEntity<Map<String, Object>> sendPrivateAudioMessage(
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
            response.put("data", message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送音频消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== 群聊专用接口 ====================

    /**
     * 发送群聊文本消息
     */
    @PostMapping("/group/send-text")
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
            response.put("data", message);
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
    @PostMapping("/group/send-image")
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
            response.put("data", message);
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
    @PostMapping("/group/send-file")
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
            response.put("data", message);
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
    @PostMapping("/group/send-video")
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
            response.put("data", message);
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
    @PostMapping("/group/send-audio")
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
            response.put("data", message);
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
    @PostMapping("/group/send-system")
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
            response.put("data", message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送群聊系统消息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 健康检查接口
     *
     * <p>用于运维或监控系统检查 IM-message-server 是否处于可用状态。</p>
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "IM-message-server");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

}
