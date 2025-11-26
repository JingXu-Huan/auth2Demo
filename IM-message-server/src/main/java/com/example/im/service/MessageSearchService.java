package com.example.im.service;

import com.example.im.entity.Message;
import com.example.im.entity.MessageSearchIndex;
import com.example.im.mapper.MessageMapper;
import com.example.im.mapper.MessageSearchIndexMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息搜索服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSearchService {

    private final MessageSearchIndexMapper searchIndexMapper;
    private final MessageMapper messageMapper;

    /**
     * 索引消息
     */
    public void indexMessage(Message message) {
        if (message.getContent() == null || message.getContent().isBlank()) {
            return;
        }
        
        MessageSearchIndex index = new MessageSearchIndex()
                .setMessageId(message.getMessageId())
                .setChannelId(message.getChannelId())
                .setSenderId(message.getSenderId())
                .setCreatedAt(LocalDateTime.now());
        
        try {
            searchIndexMapper.insert(index);
            log.debug("消息已索引: messageId={}", message.getMessageId());
        } catch (Exception e) {
            log.warn("消息索引失败: messageId={}, error={}", message.getMessageId(), e.getMessage());
        }
    }

    /**
     * 在频道内搜索消息
     */
    public List<Message> searchInChannel(Long channelId, String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        
        List<Long> messageIds = searchIndexMapper.searchInChannel(channelId, keyword, limit);
        if (messageIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return messageIds.stream()
                .map(messageMapper::selectById)
                .filter(m -> m != null)
                .collect(Collectors.toList());
    }

    /**
     * 搜索用户发送的消息
     */
    public List<Message> searchByUser(Long userId, String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        
        List<Long> messageIds = searchIndexMapper.searchByUser(userId, keyword, limit);
        if (messageIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return messageIds.stream()
                .map(messageMapper::selectById)
                .filter(m -> m != null)
                .collect(Collectors.toList());
    }

    /**
     * 删除消息索引
     */
    public void removeIndex(Long messageId) {
        searchIndexMapper.deleteById(messageId);
    }
}
