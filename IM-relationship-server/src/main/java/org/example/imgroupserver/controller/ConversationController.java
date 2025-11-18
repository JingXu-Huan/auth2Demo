package org.example.imgroupserver.controller;

import com.example.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * 创建或获取单聊会话
     */
    @PostMapping("/p2p")
    public ResponseEntity<Result<Map<String, Object>>> createOrGetP2PConversation(@RequestParam("userAId") Long userAId,
                                                                                   @RequestParam("userBId") Long userBId) {
        Map<String, Object> data = conversationService.createOrGetP2PConversation(userAId, userBId);
        return ResponseEntity.ok(Result.success("success", data));
    }

    /**
     * 创建或获取群聊会话
     */
    @PostMapping("/group/{groupId}")
    public ResponseEntity<Result<Map<String, Object>>> createOrGetGroupConversation(@PathVariable("groupId") String groupId) {
        Map<String, Object> data = conversationService.createOrGetGroupConversation(groupId);
        return ResponseEntity.ok(Result.success("success", data));
    }

    /**
     * 获取会话成员列表
     */
    @GetMapping("/{conversationId}/members")
    public ResponseEntity<Result<Map<String, Object>>> getConversationMembers(@PathVariable("conversationId") String conversationId) {
        List<Long> members = conversationService.getConversationMembers(conversationId);
        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversationId);
        data.put("members", members);
        return ResponseEntity.ok(Result.success("success", data));
    }
}
