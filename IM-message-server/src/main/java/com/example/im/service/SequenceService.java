package com.example.im.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 序号生成服务
 * 为每个会话生成严格单调递增的 SeqID
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SequenceService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Redis Key 前缀
     */
    private static final String SEQ_KEY_PREFIX = "im:channel:seq:";

    /**
     * 获取下一个序号
     * @param channelId 会话ID
     * @return 严格递增的序号
     */
    public Long getNextSeqId(Long channelId) {
        String key = SEQ_KEY_PREFIX + channelId;
        Long seqId = redisTemplate.opsForValue().increment(key);
        log.debug("生成SeqID: channelId={}, seqId={}", channelId, seqId);
        return seqId;
    }

    /**
     * 获取当前序号（不递增）
     * @param channelId 会话ID
     * @return 当前序号
     */
    public Long getCurrentSeqId(Long channelId) {
        String key = SEQ_KEY_PREFIX + channelId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    /**
     * 初始化会话序号
     * @param channelId 会话ID
     * @param initialSeq 初始序号
     */
    public void initSeqId(Long channelId, Long initialSeq) {
        String key = SEQ_KEY_PREFIX + channelId;
        redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(initialSeq));
    }
}
