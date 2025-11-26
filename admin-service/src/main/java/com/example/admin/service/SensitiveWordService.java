package com.example.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.entity.SensitiveWord;
import com.example.admin.mapper.SensitiveWordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 敏感词服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordService {
    
    private final SensitiveWordMapper sensitiveWordMapper;
    private final StringRedisTemplate redisTemplate;
    
    private static final String CHANNEL_CONFIG_UPDATE = "SYS_CONFIG_UPDATE";
    
    /**
     * 添加敏感词
     */
    @Transactional
    public void addWord(String word, String category, int actionType, Long adminId) {
        SensitiveWord entity = new SensitiveWord()
                .setWord(word)
                .setCategory(category)
                .setActionType(actionType)
                .setCreatedBy(adminId)
                .setUpdatedAt(LocalDateTime.now());
        sensitiveWordMapper.insert(entity);
        
        // 广播更新通知
        broadcastUpdate();
        log.info("添加敏感词: {}, 操作人: {}", word, adminId);
    }
    
    /**
     * 批量添加敏感词
     */
    @Transactional
    public void addWords(List<String> words, String category, int actionType, Long adminId) {
        for (String word : words) {
            SensitiveWord entity = new SensitiveWord()
                    .setWord(word)
                    .setCategory(category)
                    .setActionType(actionType)
                    .setCreatedBy(adminId)
                    .setUpdatedAt(LocalDateTime.now());
            sensitiveWordMapper.insert(entity);
        }
        broadcastUpdate();
        log.info("批量添加敏感词: {} 个, 操作人: {}", words.size(), adminId);
    }
    
    /**
     * 删除敏感词
     */
    @Transactional
    public void deleteWord(Long id, Long adminId) {
        sensitiveWordMapper.deleteById(id);
        broadcastUpdate();
        log.info("删除敏感词ID: {}, 操作人: {}", id, adminId);
    }
    
    /**
     * 分页查询敏感词
     */
    public Page<SensitiveWord> listWords(int page, int size, String category) {
        LambdaQueryWrapper<SensitiveWord> query = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            query.eq(SensitiveWord::getCategory, category);
        }
        query.orderByDesc(SensitiveWord::getUpdatedAt);
        return sensitiveWordMapper.selectPage(new Page<>(page, size), query);
    }
    
    /**
     * 获取所有敏感词（用于其他服务加载）
     */
    public List<String> getAllWords() {
        return sensitiveWordMapper.findAllWords();
    }
    
    /**
     * 广播配置更新
     */
    private void broadcastUpdate() {
        try {
            redisTemplate.convertAndSend(CHANNEL_CONFIG_UPDATE, "SENSITIVE_WORDS");
        } catch (Exception e) {
            log.error("广播敏感词更新失败", e);
        }
    }
}
