package com.example.relationship.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.relationship.dto.FriendApplyRequest;
import com.example.relationship.dto.FriendAuditRequest;
import com.example.relationship.dto.FriendRequestVO;
import com.example.relationship.entity.FriendRelation;
import com.example.relationship.entity.FriendRequest;
import com.example.relationship.mapper.BlacklistMapper;
import com.example.relationship.mapper.FriendRelationMapper;
import com.example.relationship.mapper.FriendRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 好友申请服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRequestService {
    
    private final FriendRequestMapper friendRequestMapper;
    private final FriendRelationMapper friendRelationMapper;
    private final BlacklistMapper blacklistMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    
    private static final String CACHE_PENDING_COUNT = "friend:pending:count:";
    private static final String RATE_LIMIT_KEY = "friend:apply:limit:";
    
    /**
     * 发起好友申请
     */
    @Transactional
    public Long applyFriend(Long userId, FriendApplyRequest request) {
        Long targetUserId = request.getTargetUserId();
        
        // 1. 不能添加自己
        if (userId.equals(targetUserId)) {
            throw new RuntimeException("不能添加自己为好友");
        }
        
        // 2. 检查是否已经是好友
        FriendRelation existing = friendRelationMapper.findRelation(userId, targetUserId);
        if (existing != null) {
            throw new RuntimeException("已经是好友了");
        }
        
        // 3. 检查是否被对方拉黑
        if (blacklistMapper.findBlockedBy(userId, targetUserId) != null) {
            throw new RuntimeException("对方已将你加入黑名单");
        }
        
        // 4. 检查频率限制（1分钟内只能发送1次）
        String rateLimitKey = RATE_LIMIT_KEY + userId + ":" + targetUserId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
            throw new RuntimeException("请求过于频繁，请稍后再试");
        }
        
        // 5. 检查是否有待处理的申请
        FriendRequest pendingRequest = friendRequestMapper.findPendingBetween(userId, targetUserId);
        if (pendingRequest != null) {
            throw new RuntimeException("已有待处理的申请");
        }
        
        // 6. 检查对方是否也向我发起了申请（可以直接成为好友）
        FriendRequest reverseRequest = friendRequestMapper.findPendingBetween(targetUserId, userId);
        if (reverseRequest != null) {
            // 直接同意对方的申请
            acceptRequest(userId, reverseRequest.getId(), request.getRemark(), null);
            return reverseRequest.getId();
        }
        
        // 7. 创建申请记录
        FriendRequest friendRequest = new FriendRequest()
                .setSenderId(userId)
                .setReceiverId(targetUserId)
                .setMessage(request.getMessage())
                .setRemark(request.getRemark())
                .setSource(request.getSource())
                .setSourceId(request.getSourceId())
                .setStatus(FriendRequest.STATUS_PENDING)
                .setExpiresAt(OffsetDateTime.now().plusDays(7))
                .setCreatedAt(OffsetDateTime.now())
                .setUpdatedAt(OffsetDateTime.now());
        
        friendRequestMapper.insert(friendRequest);
        
        // 8. 设置频率限制
        redisTemplate.opsForValue().set(rateLimitKey, "1", 1, TimeUnit.MINUTES);
        
        // 9. 清除对方的待处理数量缓存
        redisTemplate.delete(CACHE_PENDING_COUNT + targetUserId);
        
        // 10. 发送通知消息到MQ
        sendNotification(targetUserId, "NEW_FRIEND_REQUEST", friendRequest.getId());
        
        log.info("好友申请创建成功: {} -> {}", userId, targetUserId);
        return friendRequest.getId();
    }
    
    /**
     * 审核好友申请
     */
    @Transactional
    public void auditRequest(Long userId, FriendAuditRequest request) {
        FriendRequest friendRequest = friendRequestMapper.selectById(request.getRequestId());
        
        if (friendRequest == null) {
            throw new RuntimeException("申请不存在");
        }
        
        if (!friendRequest.getReceiverId().equals(userId)) {
            throw new RuntimeException("无权操作此申请");
        }
        
        if (friendRequest.getStatus() != FriendRequest.STATUS_PENDING) {
            throw new RuntimeException("申请已被处理");
        }
        
        if (friendRequest.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("申请已过期");
        }
        
        switch (request.getAction()) {
            case FriendAuditRequest.ACTION_ACCEPT:
                acceptRequest(userId, request.getRequestId(), request.getRemark(), request.getGroupId());
                break;
            case FriendAuditRequest.ACTION_REJECT:
                rejectRequest(userId, request.getRequestId(), request.getRejectReason());
                break;
            case FriendAuditRequest.ACTION_IGNORE:
                ignoreRequest(userId, request.getRequestId());
                break;
            default:
                throw new RuntimeException("无效的操作类型");
        }
    }
    
    /**
     * 同意好友申请
     */
    private void acceptRequest(Long userId, Long requestId, String remark, Long groupId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        
        // 1. 更新申请状态
        request.setStatus(FriendRequest.STATUS_ACCEPTED)
                .setHandledBy(userId)
                .setHandledAt(OffsetDateTime.now())
                .setUpdatedAt(OffsetDateTime.now());
        friendRequestMapper.updateById(request);
        
        // 2. 创建双向好友关系
        OffsetDateTime now = OffsetDateTime.now();
        
        // 接收者 -> 发送者
        FriendRelation relation1 = new FriendRelation()
                .setUserId(request.getReceiverId())
                .setFriendId(request.getSenderId())
                .setRemark(remark != null ? remark : request.getRemark())
                .setGroupId(groupId)
                .setSource(request.getSource())
                .setBlocked(false)
                .setStarred(false)
                .setStealth(false)
                .setAllowViewMoments(true)
                .setAllowViewOnline(true)
                .setRelationshipType(FriendRelation.TYPE_NORMAL)
                .setIntimacyScore(0)
                .setCreatedAt(now)
                .setUpdatedAt(now);
        friendRelationMapper.insert(relation1);
        
        // 发送者 -> 接收者
        FriendRelation relation2 = new FriendRelation()
                .setUserId(request.getSenderId())
                .setFriendId(request.getReceiverId())
                .setRemark(request.getRemark())
                .setSource(request.getSource())
                .setBlocked(false)
                .setStarred(false)
                .setStealth(false)
                .setAllowViewMoments(true)
                .setAllowViewOnline(true)
                .setRelationshipType(FriendRelation.TYPE_NORMAL)
                .setIntimacyScore(0)
                .setCreatedAt(now)
                .setUpdatedAt(now);
        friendRelationMapper.insert(relation2);
        
        // 3. 清除缓存
        clearFriendCache(request.getSenderId());
        clearFriendCache(request.getReceiverId());
        
        // 4. 发送MQ消息（创建会话、通知双方等）
        sendFriendAddedEvent(request.getSenderId(), request.getReceiverId());
        
        log.info("好友申请已同意: {} <-> {}", request.getSenderId(), request.getReceiverId());
    }
    
    /**
     * 拒绝好友申请
     */
    private void rejectRequest(Long userId, Long requestId, String reason) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        request.setStatus(FriendRequest.STATUS_REJECTED)
                .setHandledBy(userId)
                .setHandledAt(OffsetDateTime.now())
                .setRejectReason(reason)
                .setUpdatedAt(OffsetDateTime.now());
        friendRequestMapper.updateById(request);
        
        log.info("好友申请已拒绝: requestId={}", requestId);
    }
    
    /**
     * 忽略好友申请
     */
    private void ignoreRequest(Long userId, Long requestId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        request.setStatus(FriendRequest.STATUS_IGNORED)
                .setHandledBy(userId)
                .setHandledAt(OffsetDateTime.now())
                .setUpdatedAt(OffsetDateTime.now());
        friendRequestMapper.updateById(request);
        
        log.info("好友申请已忽略: requestId={}", requestId);
    }
    
    /**
     * 获取待处理的好友申请列表
     */
    public List<FriendRequestVO> getPendingRequests(Long userId) {
        List<FriendRequest> requests = friendRequestMapper.findPendingRequests(userId);
        return convertToVO(requests);
    }
    
    /**
     * 获取已发送的好友申请列表
     */
    public List<FriendRequestVO> getSentRequests(Long userId, int limit) {
        List<FriendRequest> requests = friendRequestMapper.findSentRequests(userId, limit);
        return convertToVO(requests);
    }
    
    /**
     * 获取待处理申请数量
     */
    public int getPendingCount(Long userId) {
        String cacheKey = CACHE_PENDING_COUNT + userId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (Integer) cached;
        }
        
        int count = friendRequestMapper.countPending(userId);
        redisTemplate.opsForValue().set(cacheKey, count, 5, TimeUnit.MINUTES);
        return count;
    }
    
    private List<FriendRequestVO> convertToVO(List<FriendRequest> requests) {
        List<FriendRequestVO> voList = new ArrayList<>();
        for (FriendRequest request : requests) {
            FriendRequestVO vo = new FriendRequestVO();
            vo.setId(request.getId());
            vo.setSenderId(request.getSenderId());
            vo.setReceiverId(request.getReceiverId());
            vo.setMessage(request.getMessage());
            vo.setRemark(request.getRemark());
            vo.setStatus(request.getStatus());
            vo.setStatusText(getStatusText(request.getStatus()));
            vo.setSource(request.getSource());
            vo.setExpiresAt(request.getExpiresAt());
            vo.setCreatedAt(request.getCreatedAt());
            vo.setExpired(request.getExpiresAt().isBefore(OffsetDateTime.now()));
            voList.add(vo);
        }
        return voList;
    }
    
    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "已同意";
            case 2 -> "已拒绝";
            case 3 -> "已忽略";
            case 4 -> "已过期";
            default -> "未知";
        };
    }
    
    private void clearFriendCache(Long userId) {
        redisTemplate.delete(CACHE_PENDING_COUNT + userId);
        redisTemplate.delete("friend:list:" + userId);
        redisTemplate.delete("friend:ids:" + userId);
    }
    
    private void sendNotification(Long userId, String type, Long requestId) {
        try {
            String message = String.format("{\"userId\":%d,\"type\":\"%s\",\"requestId\":%d}", userId, type, requestId);
            rocketMQTemplate.convertAndSend("friend_notification_topic", message);
        } catch (Exception e) {
            log.error("发送通知失败", e);
        }
    }
    
    private void sendFriendAddedEvent(Long userId1, Long userId2) {
        try {
            String message = String.format("{\"userId1\":%d,\"userId2\":%d,\"event\":\"FRIEND_ADDED\"}", userId1, userId2);
            rocketMQTemplate.convertAndSend("friend_event_topic", message);
        } catch (Exception e) {
            log.error("发送好友添加事件失败", e);
        }
    }
}
