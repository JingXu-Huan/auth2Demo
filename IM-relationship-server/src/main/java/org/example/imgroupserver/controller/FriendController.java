package org.example.imgroupserver.controller;

import com.example.domain.dto.AddFriendRequest;
import com.example.domain.dto.FriendDTO;
import com.example.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.feign.UserServiceClient;
import org.example.imgroupserver.service.FriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 好友管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {
    
    private final FriendService friendService;
    private final UserServiceClient userServiceClient;
    
    /**
     * 发送好友请求
     */
    @PostMapping("/request")
    public ResponseEntity<Result<Long>> sendFriendRequest(@Valid @RequestBody AddFriendRequest request) {
        try {
            Long requestId = friendService.sendFriendRequest(request);
            return ResponseEntity.ok(Result.success("好友请求已发送", requestId));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("发送好友请求失败", e);
            return ResponseEntity.ok(Result.error(500, "发送好友请求失败"));
        }
    }
    
    /**
     * 接受好友请求
     */
    @PostMapping("/request/accept")
    public ResponseEntity<Result<Void>> acceptFriendRequest(
            @RequestParam Long fromUserId,
            @RequestParam Long toUserId) {
        try {
            friendService.acceptFriendRequest(fromUserId, toUserId);
            return ResponseEntity.ok(Result.success("已接受好友请求"));
        } catch (Exception e) {
            log.error("接受好友请求失败", e);
            return ResponseEntity.ok(Result.error(500, "接受好友请求失败"));
        }
    }
    
    /**
     * 拒绝好友请求
     */
    @PostMapping("/request/reject")
    public ResponseEntity<Result<Void>> rejectFriendRequest(
            @RequestParam Long fromUserId,
            @RequestParam Long toUserId) {
        try {
            friendService.rejectFriendRequest(fromUserId, toUserId);
            return ResponseEntity.ok(Result.success("已拒绝好友请求"));
        } catch (Exception e) {
            log.error("拒绝好友请求失败", e);
            return ResponseEntity.ok(Result.error(500, "拒绝好友请求失败"));
        }
    }
    
    /**
     * 获取收到的好友请求列表
     */
    @GetMapping("/requests/received/{userId}")
    public ResponseEntity<Result<List<com.example.domain.dto.FriendRequestDTO>>> getReceivedRequests(
            @PathVariable Long userId) {
        try {
            List<com.example.domain.dto.FriendRequestDTO> requests = friendService.getReceivedRequests(userId);
            return ResponseEntity.ok(Result.success("获取成功", requests));
        } catch (Exception e) {
            log.error("获取好友请求列表失败", e);
            return ResponseEntity.ok(Result.error(500, "获取好友请求列表失败"));
        }
    }
    
    /**
     * 获取好友列表
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Result<List<FriendDTO>>> getFriends(@PathVariable Long userId) {
        try {
            List<FriendDTO> friends = friendService.getFriends(userId);
            return ResponseEntity.ok(Result.success("获取好友列表成功", friends));
        } catch (Exception e) {
            log.error("获取好友列表失败", e);
            return ResponseEntity.ok(Result.error(500, "获取好友列表失败"));
        }
    }
    
    /**
     * 删除好友
     */
    @DeleteMapping("/{userId}/friends/{friendId}")
    public ResponseEntity<Result<Void>> deleteFriend(
            @PathVariable Long userId,
            @PathVariable Long friendId) {
        try {
            friendService.deleteFriend(userId, friendId);
            return ResponseEntity.ok(Result.success("删除好友成功"));
        } catch (Exception e) {
            log.error("删除好友失败", e);
            return ResponseEntity.ok(Result.error(500, "删除好友失败"));
        }
    }
    
    /**
     * 搜索好友
     */
    @GetMapping("/{userId}/search")
    public ResponseEntity<Result<List<FriendDTO>>> searchFriends(
            @PathVariable Long userId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<FriendDTO> friends = friendService.searchFriends(userId, keyword, limit);
            return ResponseEntity.ok(Result.success("搜索成功", friends));
        } catch (Exception e) {
            log.error("搜索好友失败", e);
            return ResponseEntity.ok(Result.error(500, "搜索好友失败"));
        }
    }
    
    /**
     * 搜索用户（通过邮箱或手机号）
     */
    @GetMapping("/search-user")
    public ResponseEntity<Result<Map<String, Object>>> searchUser(
            @RequestParam String searchType,
            @RequestParam String keyword) {
        try {
            // 调用 User-server 搜索用户
            // 注意：这里直接透传给 User-server，由 User-server 处理搜索逻辑
            log.info("搜索用户: searchType={}, keyword={}", searchType, keyword);
            
            // 由于 UserServiceClient 只有 getUserById，这里暂时返回提示
            // 实际应该添加一个 searchUser 的 Feign 方法
            return ResponseEntity.ok(Result.error(501, "搜索用户功能需要通过 User-server 的 /api/v1/users/search 接口调用"));
        } catch (Exception e) {
            log.error("搜索用户失败", e);
            return ResponseEntity.ok(Result.error(500, "搜索用户失败"));
        }
    }
}
