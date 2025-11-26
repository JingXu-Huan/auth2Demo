package com.example.relationship.controller;

import com.example.common.result.Result;
import com.example.relationship.entity.Follow;
import com.example.relationship.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关注关系接口
 */
@RestController
@RequestMapping("/api/relationship/follows")
@RequiredArgsConstructor
public class FollowController {
    
    private final FollowService followService;
    
    /**
     * 关注用户
     */
    @PostMapping("/{targetId}")
    public Result<Void> follow(
            @PathVariable Long targetId,
            @RequestHeader("X-User-Id") Long userId) {
        followService.follow(userId, targetId);
        return Result.success(null);
    }
    
    /**
     * 取消关注
     */
    @DeleteMapping("/{targetId}")
    public Result<Void> unfollow(
            @PathVariable Long targetId,
            @RequestHeader("X-User-Id") Long userId) {
        followService.unfollow(userId, targetId);
        return Result.success(null);
    }
    
    /**
     * 设置特别关注
     */
    @PutMapping("/{targetId}/special")
    public Result<Void> setSpecialFollow(
            @PathVariable Long targetId,
            @RequestParam boolean special,
            @RequestHeader("X-User-Id") Long userId) {
        followService.setSpecialFollow(userId, targetId, special);
        return Result.success(null);
    }
    
    /**
     * 获取关注列表
     */
    @GetMapping("/following")
    public Result<List<Follow>> getFollowing(@RequestHeader("X-User-Id") Long userId) {
        List<Follow> list = followService.getFollowing(userId);
        return Result.success(list);
    }
    
    /**
     * 获取粉丝列表
     */
    @GetMapping("/followers")
    public Result<List<Follow>> getFollowers(@RequestHeader("X-User-Id") Long userId) {
        List<Follow> list = followService.getFollowers(userId);
        return Result.success(list);
    }
    
    /**
     * 检查关注状态
     */
    @GetMapping("/check/{targetId}")
    public Result<Map<String, Boolean>> checkFollowStatus(
            @PathVariable Long targetId,
            @RequestHeader("X-User-Id") Long userId) {
        boolean isFollowing = followService.isFollowing(userId, targetId);
        boolean isMutual = followService.isMutualFollow(userId, targetId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("isFollowing", isFollowing);
        result.put("isMutual", isMutual);
        return Result.success(result);
    }
    
    /**
     * 获取关注统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Integer>> getFollowStats(@RequestHeader("X-User-Id") Long userId) {
        int followingCount = followService.getFollowing(userId).size();
        int followerCount = followService.getFollowers(userId).size();
        Map<String, Integer> result = new HashMap<>();
        result.put("followingCount", followingCount);
        result.put("followerCount", followerCount);
        return Result.success(result);
    }
}
