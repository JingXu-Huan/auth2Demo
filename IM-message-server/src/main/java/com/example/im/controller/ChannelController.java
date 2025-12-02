package com.example.im.controller;

import com.example.domain.vo.Result;
import com.example.im.entity.Channel;
import com.example.im.entity.ChannelMember;
import com.example.im.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 频道/会话管理接口
 */
@RestController
@RequestMapping("/api/v1/im/channels")
@RequiredArgsConstructor
public class ChannelController {
    
    private final ChannelService channelService;
    
    /**
     * 创建频道
     */
    @PostMapping
    public Result<Channel> createChannel(
            @RequestBody Channel channel,
            @RequestHeader(value = "X-User-Id") Long userId) {
        Channel created = channelService.createChannel(channel, userId);
        return Result.success(created);
    }
    
    /**
     * 获取频道详情
     */
    @GetMapping("/{channelId}")
    public Result<Channel> getChannel(@PathVariable("channelId") Long channelId) {
        Channel channel = channelService.getChannelById(channelId);
        return Result.success(channel);
    }
    
    /**
     * 更新频道信息
     */
    @PutMapping("/{channelId}")
    public Result<Channel> updateChannel(
            @PathVariable("channelId") Long channelId,
            @RequestBody Channel channel) {
        Channel updated = channelService.updateChannel(channelId, channel);
        return Result.success(updated);
    }
    
    /**
     * 删除/解散频道
     */
    @DeleteMapping("/{channelId}")
    public Result<Void> deleteChannel(
            @PathVariable("channelId") Long channelId,
            @RequestHeader(value = "X-User-Id") Long userId) {
        channelService.deleteChannel(channelId, userId);
        return Result.success(null);
    }
    
    /**
     * 获取用户的频道列表
     */
    @GetMapping("/user")
    public Result<List<Channel>> getUserChannels(@RequestHeader(value = "X-User-Id") Long userId) {
        List<Channel> channels = channelService.getUserChannels(userId);
        return Result.success(channels);
    }
    
    /**
     * 添加成员
     */
    @PostMapping("/{channelId}/members")
    public Result<Void> addMembers(
            @PathVariable("channelId") Long channelId,
            @RequestBody List<Long> memberIds,
            @RequestHeader(value = "X-User-Id") Long operatorId) {
        channelService.addMembers(channelId, memberIds, operatorId);
        return Result.success(null);
    }
    
    /**
     * 移除成员
     */
    @DeleteMapping("/{channelId}/members/{memberId}")
    public Result<Void> removeMember(
            @PathVariable("channelId") Long channelId,
            @PathVariable("memberId") Long memberId,
            @RequestHeader(value = "X-User-Id") Long operatorId) {
        channelService.removeMember(channelId, memberId, operatorId);
        return Result.success(null);
    }
    
    /**
     * 获取频道成员列表
     */
    @GetMapping("/{channelId}/members")
    public Result<List<ChannelMember>> getMembers(@PathVariable("channelId") Long channelId) {
        List<ChannelMember> members = channelService.getChannelMembers(channelId);
        return Result.success(members);
    }
    
    /**
     * 退出频道
     */
    @PostMapping("/{channelId}/leave")
    public Result<Void> leaveChannel(
            @PathVariable("channelId") Long channelId,
            @RequestHeader(value = "X-User-Id") Long userId) {
        channelService.leaveChannel(channelId, userId);
        return Result.success(null);
    }
    
    /**
     * 设置管理员
     */
    @PostMapping("/{channelId}/admins/{memberId}")
    public Result<Void> setAdmin(
            @PathVariable("channelId") Long channelId,
            @PathVariable("memberId") Long memberId,
            @RequestParam("isAdmin") boolean isAdmin,
            @RequestHeader(value = "X-User-Id") Long operatorId) {
        channelService.setAdmin(channelId, memberId, isAdmin, operatorId);
        return Result.success(null);
    }
    
    /**
     * 获取或创建私聊频道
     */
    @PostMapping("/private/{targetUserId}")
    public Result<Channel> getOrCreatePrivateChannel(
            @PathVariable("targetUserId") Long targetUserId,
            @RequestHeader(value = "X-User-Id") Long userId) {
        Channel channel = channelService.getOrCreatePrivateChannel(userId, targetUserId);
        return Result.success(channel);
    }
}
