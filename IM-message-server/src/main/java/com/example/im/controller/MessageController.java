package com.example.im.controller;

import com.example.common.result.Result;
import com.example.im.dto.SendMessageRequest;
import com.example.im.entity.Message;
import com.example.im.service.MessageService;
import com.example.im.service.MessageSearchService;
import com.example.im.entity.MessageReaction;
import com.example.im.mapper.MessageReactionMapper;
import com.example.im.mapper.MessageReadMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final MessageSearchService messageSearchService;
    private final MessageReactionMapper reactionMapper;
    private final MessageReadMapper readMapper;

    /**
     * 发送消息
     */
    @PostMapping("/messages")
    public Result<Long> sendMessage(
            @RequestHeader(value = "X-User-Id") Long userId,
            @Valid @RequestBody SendMessageRequest request) {
        Long messageId = messageService.sendMessage(userId, request);
        return Result.success(messageId);
    }

    /**
     * 同步消息（拉取历史消息）
     */
    @GetMapping("/messages/sync")
    public Result<List<Message>> syncMessages(
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestParam("channelId") Long channelId,
            @RequestParam(value = "cursor", defaultValue = "0") Long cursor,
            @RequestParam(value = "limit", defaultValue = "50") Integer limit) {
        List<Message> messages = messageService.syncMessages(userId, channelId, cursor, limit);
        return Result.success(messages);
    }

    /**
     * 撤回消息
     */
    @PostMapping("/messages/{messageId}/recall")
    public Result<Void> recallMessage(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable("messageId") Long messageId) {
        messageService.recallMessage(userId, messageId);
        return Result.success();
    }

    /**
     * 标记消息已读
     */
    @PostMapping("/messages/read")
    public Result<Void> markAsRead(
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestParam("channelId") Long channelId,
            @RequestParam("messageId") Long messageId) {
        readMapper.markAsRead(messageId, channelId, userId);
        return Result.success();
    }

    /**
     * 获取已读回执
     */
    @GetMapping("/messages/{messageId}/reads")
    public Result<Map<String, Object>> getReadReceipts(
            @PathVariable("messageId") Long messageId,
            @RequestParam("channelId") Long channelId) {
        int readCount = readMapper.countReads(messageId);
        List<Long> readerIds = readMapper.getReaderIds(messageId, 50);
        
        Map<String, Object> result = new HashMap<>();
        result.put("readCount", readCount);
        result.put("readerIds", readerIds);
        return Result.success(result);
    }

    /**
     * 添加表情反应
     */
    @PostMapping("/messages/{messageId}/reactions")
    public Result<Void> addReaction(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable("messageId") Long messageId,
            @RequestParam("emoji") String emoji) {
        MessageReaction reaction = new MessageReaction()
                .setMessageId(messageId)
                .setUserId(userId)
                .setEmoji(emoji);
        try {
            reactionMapper.insert(reaction);
        } catch (Exception e) {
            // 已存在则忽略
        }
        return Result.success();
    }

    /**
     * 移除表情反应
     */
    @DeleteMapping("/messages/{messageId}/reactions")
    public Result<Void> removeReaction(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable("messageId") Long messageId,
            @RequestParam("emoji") String emoji) {
        reactionMapper.deleteReaction(messageId, userId, emoji);
        return Result.success();
    }

    /**
     * 获取消息反应列表
     */
    @GetMapping("/messages/{messageId}/reactions")
    public Result<List<MessageReaction>> getReactions(@PathVariable("messageId") Long messageId) {
        List<MessageReaction> reactions = reactionMapper.findByMessage(messageId);
        return Result.success(reactions);
    }

    /**
     * 搜索消息
     */
    @GetMapping("/messages/search")
    public Result<List<Message>> searchMessages(
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestParam("channelId") Long channelId,
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        List<Message> messages = messageSearchService.searchInChannel(channelId, keyword, limit);
        return Result.success(messages);
    }

    /**
     * 转发消息
     */
    @PostMapping("/messages/{messageId}/forward")
    public Result<Long> forwardMessage(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable("messageId") Long messageId,
            @RequestParam("targetChannelId") Long targetChannelId) {
        // 获取原消息并转发
        SendMessageRequest request = new SendMessageRequest();
        request.setChannelId(targetChannelId);
        request.setForwardFromMsgId(messageId);
        request.setMsgType(1); // 文本类型
        request.setContent("[转发消息]");
        
        Long newMessageId = messageService.sendMessage(userId, request);
        return Result.success(newMessageId);
    }

    /**
     * 获取@我的消息
     */
    @GetMapping("/messages/mentions")
    public Result<List<Message>> getMentionedMessages(
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestParam(value = "cursor", defaultValue = "0") Long cursor,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        // TODO: 实现@消息查询
        return Result.success(Collections.emptyList());
    }
}
