package org.example.imgroupserver.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.service.FriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 内部接口：用于在 Neo4j 中初始化用户节点
 * 供 User-server 在用户注册成功后调用
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/relationship/internal/users")
@RequiredArgsConstructor
public class UserNodeController {

    private final FriendService friendService;

    @PostMapping("/{userId}/ensure-node")
    public ResponseEntity<Void> ensureUserNode(@PathVariable("userId") Long userId) {
        try {
            friendService.ensureUserNodeExists(userId);
            log.info("确保 Neo4j 用户节点存在: userId={}", userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("初始化 Neo4j 用户节点失败: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
