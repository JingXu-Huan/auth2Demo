package com.example.relationship.controller;

import com.example.domain.vo.Result;
import com.example.relationship.dto.*;
import com.example.relationship.entity.Blacklist;
import com.example.relationship.entity.FriendGroup;
import com.example.relationship.service.FriendRelationService;
import com.example.relationship.service.FriendRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 好友关系控制器
 */
@Tag(name = "好友关系管理")
@RestController
@RequestMapping("/api/v1/relations")
@RequiredArgsConstructor
public class FriendController {
    
    private final FriendRequestService friendRequestService;
    private final FriendRelationService friendRelationService;
    
    // ==================== 好友申请接口 ====================
    
    @Operation(summary = "发起好友申请")
    @PostMapping("/friend/apply")
    public Result<Long> applyFriend(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody FriendApplyRequest request) {
        Long requestId = friendRequestService.applyFriend(userId, request);
        return Result.success(requestId);
    }
    
    @Operation(summary = "审核好友申请")
    @PutMapping("/friend/audit")
    public Result<Void> auditRequest(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody FriendAuditRequest request) {
        friendRequestService.auditRequest(userId, request);
        return Result.success();
    }
    
    @Operation(summary = "获取待处理的好友申请")
    @GetMapping("/friend/requests/pending")
    public Result<List<FriendRequestVO>> getPendingRequests(
            @RequestHeader("X-User-Id") Long userId) {
        List<FriendRequestVO> requests = friendRequestService.getPendingRequests(userId);
        return Result.success(requests);
    }
    
    @Operation(summary = "获取已发送的好友申请")
    @GetMapping("/friend/requests/sent")
    public Result<List<FriendRequestVO>> getSentRequests(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        List<FriendRequestVO> requests = friendRequestService.getSentRequests(userId, limit);
        return Result.success(requests);
    }
    
    @Operation(summary = "获取待处理申请数量")
    @GetMapping("/friend/requests/count")
    public Result<Integer> getPendingCount(
            @RequestHeader("X-User-Id") Long userId) {
        int count = friendRequestService.getPendingCount(userId);
        return Result.success(count);
    }
    
    // ==================== 好友关系接口 ====================
    
    @Operation(summary = "获取好友列表")
    @GetMapping("/friends")
    public Result<List<FriendVO>> getFriendList(
            @RequestHeader("X-User-Id") Long userId) {
        List<FriendVO> friends = friendRelationService.getFriendList(userId);
        return Result.success(friends);
    }
    
    @Operation(summary = "获取好友ID集合")
    @GetMapping("/friends/ids")
    public Result<Set<Long>> getFriendIds(
            @RequestHeader("X-User-Id") Long userId) {
        Set<Long> friendIds = friendRelationService.getFriendIds(userId);
        return Result.success(friendIds);
    }
    
    @Operation(summary = "检查是否是好友")
    @GetMapping("/check")
    public Result<Boolean> checkFriend(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long targetId) {
        boolean isFriend = friendRelationService.isFriend(userId, targetId);
        return Result.success(isFriend);
    }
    
    @Operation(summary = "获取好友详情")
    @GetMapping("/friends/{friendId}")
    public Result<FriendVO> getFriendDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long friendId) {
        FriendVO friend = friendRelationService.getFriendDetail(userId, friendId);
        return Result.success(friend);
    }
    
    @Operation(summary = "更新好友备注")
    @PutMapping("/friends/{friendId}/remark")
    public Result<Void> updateRemark(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long friendId,
            @RequestParam String remark) {
        friendRelationService.updateRemark(userId, friendId, remark);
        return Result.success();
    }
    
    @Operation(summary = "设置/取消星标好友")
    @PostMapping("/friends/{friendId}/star")
    public Result<Void> toggleStarred(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long friendId) {
        friendRelationService.toggleStarred(userId, friendId);
        return Result.success();
    }
    
    @Operation(summary = "移动好友到分组")
    @PutMapping("/friends/{friendId}/group")
    public Result<Void> moveToGroup(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long friendId,
            @RequestParam(required = false) Long groupId) {
        friendRelationService.moveToGroup(userId, friendId, groupId);
        return Result.success();
    }
    
    @Operation(summary = "删除好友")
    @DeleteMapping("/friends/{friendId}")
    public Result<Void> deleteFriend(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long friendId) {
        friendRelationService.deleteFriend(userId, friendId);
        return Result.success();
    }
    
    @Operation(summary = "获取星标好友")
    @GetMapping("/friends/starred")
    public Result<List<FriendVO>> getStarredFriends(
            @RequestHeader("X-User-Id") Long userId) {
        List<FriendVO> friends = friendRelationService.getStarredFriends(userId);
        return Result.success(friends);
    }
    
    @Operation(summary = "获取好友统计")
    @GetMapping("/friends/stats")
    public Result<Map<String, Integer>> getFriendStats(
            @RequestHeader("X-User-Id") Long userId) {
        Map<String, Integer> stats = friendRelationService.getFriendStats(userId);
        return Result.success(stats);
    }
    
    // ==================== 黑名单接口 ====================
    
    @Operation(summary = "拉黑用户")
    @PostMapping("/blacklist/{targetId}")
    public Result<Void> blockUser(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long targetId,
            @RequestParam(required = false) String reason) {
        friendRelationService.blockFriend(userId, targetId, reason);
        return Result.success();
    }
    
    @Operation(summary = "取消拉黑")
    @DeleteMapping("/blacklist/{targetId}")
    public Result<Void> unblockUser(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long targetId) {
        friendRelationService.unblockUser(userId, targetId);
        return Result.success();
    }
    
    @Operation(summary = "获取黑名单列表")
    @GetMapping("/blacklist")
    public Result<List<Blacklist>> getBlacklist(
            @RequestHeader("X-User-Id") Long userId) {
        List<Blacklist> blacklist = friendRelationService.getBlacklist(userId);
        return Result.success(blacklist);
    }
    
    // ==================== 好友分组接口 ====================
    
    @Operation(summary = "创建好友分组")
    @PostMapping("/groups")
    public Result<Long> createGroup(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String name) {
        Long groupId = friendRelationService.createGroup(userId, name);
        return Result.success(groupId);
    }
    
    @Operation(summary = "获取分组列表")
    @GetMapping("/groups")
    public Result<List<FriendGroup>> getGroups(
            @RequestHeader("X-User-Id") Long userId) {
        List<FriendGroup> groups = friendRelationService.getGroups(userId);
        return Result.success(groups);
    }
    
    @Operation(summary = "删除分组")
    @DeleteMapping("/groups/{groupId}")
    public Result<Void> deleteGroup(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long groupId) {
        friendRelationService.deleteGroup(userId, groupId);
        return Result.success();
    }
}
