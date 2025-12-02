package com.example.relationship.service;

import com.example.relationship.dto.FriendVO;
import com.example.relationship.entity.Blacklist;
import com.example.relationship.entity.FriendGroup;
import com.example.relationship.entity.FriendRelation;
import com.example.relationship.mapper.BlacklistMapper;
import com.example.relationship.mapper.FriendGroupMapper;
import com.example.relationship.mapper.FriendRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ====================================================================
 * 好友关系服务 (Friend Relation Service)
 * ====================================================================
 * 
 * 【业务场景】
 * 实现IM系统中的好友关系管理，类似于：
 * - 微信的好友列表
 * - QQ的好友系统
 * 
 * 【好友关系模型】
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    好友关系（双向）                          │
 * │                                                             │
 * │   用户A ◄─────────────────────────────────► 用户B           │
 * │          │                               │                 │
 * │          │  FriendRelation表             │                 │
 * │          │  (user_id=A, friend_id=B)    │                 │
 * │          │  (user_id=B, friend_id=A)    │  ← 存储两条记录  │
 * │          │                               │                 │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * 【为什么存储双向记录？】
 * 1. 查询效率：直接通过user_id查询我的所有好友
 * 2. 独立设置：A可以给B设置备注，B可以给A设置不同备注
 * 3. 分组管理：A把B放"同事"组，B把A放"朋友"组
 * 
 * 【功能模块】
 * - 好友列表查询（支持分组）
 * - 好友关系判断（高频操作，需缓存）
 * - 好友分组管理
 * - 黑名单管理
 * - 好友互动统计（亲密度等）
 * 
 * 【缓存策略】
 * - friend:ids:{userId} → Set<Long> 好友ID集合（判断好友用）
 * - friend:list:{userId} → List<FriendVO> 好友列表（展示用）
 * - 缓存过期时间：10分钟
 * - 好友变动时主动清除缓存
 * 
 * @author 学习笔记
 * @see FriendRequestService 好友申请服务
 * @see FriendRelation 好友关系实体
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRelationService {
    
    /** 好友关系数据访问 */
    private final FriendRelationMapper friendRelationMapper;
    
    /** 好友分组数据访问 */
    private final FriendGroupMapper friendGroupMapper;
    
    /** 黑名单数据访问 */
    private final BlacklistMapper blacklistMapper;
    
    /** Redis模板 - 好友关系缓存 */
    private final RedisTemplate<String, Object> redisTemplate;
    
    /** RocketMQ模板 - 好友事件通知 */
    private final RocketMQTemplate rocketMQTemplate;
    
    /** 好友列表缓存Key前缀 */
    private static final String CACHE_FRIEND_LIST = "friend:list:";
    
    /** 好友ID集合缓存Key前缀（高频判断用） */
    private static final String CACHE_FRIEND_IDS = "friend:ids:";
    
    /**
     * 获取好友列表
     */
    public List<FriendVO> getFriendList(Long userId) {
        List<FriendRelation> relations = friendRelationMapper.findFriends(userId);
        return convertToVO(relations);
    }
    
    /**
     * 获取好友ID集合（用于判断是否是好友）
     */
    @SuppressWarnings("unchecked")
    public Set<Long> getFriendIds(Long userId) {
        String cacheKey = CACHE_FRIEND_IDS + userId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return new HashSet<>((List<Long>) cached);
        }
        
        List<Long> friendIds = friendRelationMapper.findFriendIds(userId);
        redisTemplate.opsForValue().set(cacheKey, friendIds, 10, TimeUnit.MINUTES);
        return new HashSet<>(friendIds);
    }
    
    /**
     * 检查两人是否是好友
     */
    public boolean isFriend(Long userId, Long targetId) {
        return getFriendIds(userId).contains(targetId);
    }
    
    /**
     * 获取好友详情
     */
    public FriendVO getFriendDetail(Long userId, Long friendId) {
        FriendRelation relation = friendRelationMapper.findRelation(userId, friendId);
        if (relation == null) {
            throw new RuntimeException("不是好友关系");
        }
        return convertToVO(relation);
    }
    
    /**
     * 更新好友备注
     */
    @Transactional
    public void updateRemark(Long userId, Long friendId, String remark) {
        FriendRelation relation = friendRelationMapper.findRelation(userId, friendId);
        if (relation == null) {
            throw new RuntimeException("不是好友关系");
        }
        relation.setRemark(remark).setUpdatedAt(OffsetDateTime.now());
        friendRelationMapper.updateById(relation);
        clearCache(userId);
    }
    
    /**
     * 设置/取消星标好友
     */
    @Transactional
    public void toggleStarred(Long userId, Long friendId) {
        FriendRelation relation = friendRelationMapper.findRelation(userId, friendId);
        if (relation == null) {
            throw new RuntimeException("不是好友关系");
        }
        relation.setStarred(!relation.getStarred()).setUpdatedAt(OffsetDateTime.now());
        friendRelationMapper.updateById(relation);
        clearCache(userId);
    }
    
    /**
     * 移动好友到分组
     */
    @Transactional
    public void moveToGroup(Long userId, Long friendId, Long groupId) {
        FriendRelation relation = friendRelationMapper.findRelation(userId, friendId);
        if (relation == null) {
            throw new RuntimeException("不是好友关系");
        }
        
        // 验证分组存在
        if (groupId != null) {
            FriendGroup group = friendGroupMapper.selectById(groupId);
            if (group == null || !group.getUserId().equals(userId)) {
                throw new RuntimeException("分组不存在");
            }
        }
        
        relation.setGroupId(groupId).setUpdatedAt(OffsetDateTime.now());
        friendRelationMapper.updateById(relation);
        clearCache(userId);
    }
    
    /**
     * 删除好友
     */
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        // 删除双向关系
        FriendRelation relation1 = friendRelationMapper.findRelation(userId, friendId);
        FriendRelation relation2 = friendRelationMapper.findRelation(friendId, userId);
        
        OffsetDateTime now = OffsetDateTime.now();
        if (relation1 != null) {
            relation1.setDeletedAt(now);
            friendRelationMapper.updateById(relation1);
        }
        if (relation2 != null) {
            relation2.setDeletedAt(now);
            friendRelationMapper.updateById(relation2);
        }
        
        // 清除缓存
        clearCache(userId);
        clearCache(friendId);
        
        // 发送好友删除事件
        sendFriendDeletedEvent(userId, friendId);
        
        log.info("好友关系已删除: {} <-> {}", userId, friendId);
    }
    
    /**
     * 拉黑好友
     */
    @Transactional
    public void blockFriend(Long userId, Long friendId, String reason) {
        // 1. 检查是否已拉黑
        if (blacklistMapper.findBlocked(userId, friendId) != null) {
            throw new RuntimeException("已在黑名单中");
        }
        
        // 2. 添加到黑名单
        Blacklist blacklist = new Blacklist()
                .setUserId(userId)
                .setBlockedUserId(friendId)
                .setReason(reason)
                .setBlockMessages(true)
                .setBlockCalls(true)
                .setBlockMoments(true)
                .setCreatedAt(OffsetDateTime.now());
        blacklistMapper.insert(blacklist);
        
        // 3. 删除好友关系（单向）
        FriendRelation relation = friendRelationMapper.findRelation(userId, friendId);
        if (relation != null) {
            relation.setBlocked(true).setDeletedAt(OffsetDateTime.now());
            friendRelationMapper.updateById(relation);
        }
        
        clearCache(userId);
        log.info("用户已被拉黑: {} blocked {}", userId, friendId);
    }
    
    /**
     * 取消拉黑
     */
    @Transactional
    public void unblockUser(Long userId, Long blockedUserId) {
        Blacklist blacklist = blacklistMapper.findBlocked(userId, blockedUserId);
        if (blacklist == null) {
            throw new RuntimeException("不在黑名单中");
        }
        blacklistMapper.deleteById(blacklist.getId());
        clearCache(userId);
        log.info("取消拉黑: {} unblocked {}", userId, blockedUserId);
    }
    
    /**
     * 获取黑名单列表
     */
    public List<Blacklist> getBlacklist(Long userId) {
        return blacklistMapper.findByUser(userId);
    }
    
    /**
     * 获取星标好友
     */
    public List<FriendVO> getStarredFriends(Long userId) {
        List<FriendRelation> relations = friendRelationMapper.findStarredFriends(userId);
        return convertToVO(relations);
    }
    
    /**
     * 获取好友统计
     */
    public Map<String, Integer> getFriendStats(Long userId) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", friendRelationMapper.countFriends(userId));
        stats.put("starred", friendRelationMapper.findStarredFriends(userId).size());
        stats.put("blocked", blacklistMapper.findByUser(userId).size());
        return stats;
    }
    
    // ========== 好友分组管理 ==========
    
    /**
     * 创建好友分组
     */
    @Transactional
    public Long createGroup(Long userId, String name) {
        // 检查分组数量限制
        List<FriendGroup> groups = friendGroupMapper.findByUser(userId);
        if (groups.size() >= 20) {
            throw new RuntimeException("分组数量已达上限");
        }
        
        FriendGroup group = new FriendGroup()
                .setUserId(userId)
                .setName(name)
                .setIsDefault(false)
                .setSortOrder(groups.size())
                .setMemberCount(0)
                .setCreatedAt(OffsetDateTime.now())
                .setUpdatedAt(OffsetDateTime.now());
        friendGroupMapper.insert(group);
        return group.getId();
    }
    
    /**
     * 获取分组列表
     */
    public List<FriendGroup> getGroups(Long userId) {
        return friendGroupMapper.findByUser(userId);
    }
    
    /**
     * 删除分组
     */
    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        FriendGroup group = friendGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new RuntimeException("分组不存在");
        }
        if (group.getIsDefault()) {
            throw new RuntimeException("默认分组不能删除");
        }
        
        // 将该分组的好友移到默认分组
        FriendGroup defaultGroup = friendGroupMapper.findDefault(userId);
        if (defaultGroup != null) {
            // 更新好友的分组ID
            List<FriendRelation> relations = friendRelationMapper.findByGroup(userId, groupId);
            for (FriendRelation relation : relations) {
                relation.setGroupId(defaultGroup.getId());
                friendRelationMapper.updateById(relation);
            }
        }
        
        friendGroupMapper.deleteById(groupId);
        clearCache(userId);
    }
    
    private List<FriendVO> convertToVO(List<FriendRelation> relations) {
        return relations.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    private FriendVO convertToVO(FriendRelation relation) {
        FriendVO vo = new FriendVO();
        vo.setUserId(relation.getUserId());
        vo.setFriendId(relation.getFriendId());
        vo.setRemark(relation.getRemark());
        vo.setTags(relation.getTags());
        vo.setGroupId(relation.getGroupId());
        vo.setStarred(relation.getStarred());
        vo.setBlocked(relation.getBlocked());
        vo.setRelationshipType(relation.getRelationshipType());
        vo.setIntimacyScore(relation.getIntimacyScore());
        vo.setCreatedAt(relation.getCreatedAt());
        return vo;
    }
    
    private void clearCache(Long userId) {
        redisTemplate.delete(CACHE_FRIEND_LIST + userId);
        redisTemplate.delete(CACHE_FRIEND_IDS + userId);
    }
    
    private void sendFriendDeletedEvent(Long userId, Long friendId) {
        try {
            String message = String.format("{\"userId1\":%d,\"userId2\":%d,\"event\":\"FRIEND_DELETED\"}", userId, friendId);
            rocketMQTemplate.convertAndSend("friend_event_topic", message);
        } catch (Exception e) {
            log.error("发送好友删除事件失败", e);
        }
    }
}
