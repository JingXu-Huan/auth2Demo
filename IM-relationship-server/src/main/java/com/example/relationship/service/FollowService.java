package com.example.relationship.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.relationship.entity.Follow;
import com.example.relationship.mapper.FollowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 关注服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowService {
    
    private final FollowMapper followMapper;
    
    /**
     * 关注用户
     */
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("不能关注自己");
        }
        
        // 检查是否已关注
        if (isFollowing(followerId, followingId)) {
            return;
        }
        
        Follow follow = new Follow()
                .setFollowerId(followerId)
                .setFollowingId(followingId)
                .setSpecialFollow(false)
                .setNotificationsEnabled(true)
                .setCreatedAt(OffsetDateTime.now());
        followMapper.insert(follow);
        
        log.info("关注成功: {} -> {}", followerId, followingId);
    }
    
    /**
     * 取消关注
     */
    public void unfollow(Long followerId, Long followingId) {
        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFollowingId, followingId));
        log.info("取消关注: {} -> {}", followerId, followingId);
    }
    
    /**
     * 设置特别关注
     */
    public void setSpecialFollow(Long followerId, Long followingId, boolean special) {
        Follow follow = getFollow(followerId, followingId);
        if (follow == null) {
            throw new IllegalArgumentException("未关注该用户");
        }
        follow.setSpecialFollow(special);
        followMapper.updateById(follow);
    }
    
    /**
     * 检查是否关注
     */
    public boolean isFollowing(Long followerId, Long followingId) {
        return getFollow(followerId, followingId) != null;
    }
    
    /**
     * 获取关注关系
     */
    public Follow getFollow(Long followerId, Long followingId) {
        return followMapper.selectOne(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFollowingId, followingId));
    }
    
    /**
     * 获取关注列表(ID)
     */
    public List<Long> getFollowingIds(Long userId) {
        return followMapper.selectFollowingIds(userId);
    }
    
    /**
     * 获取粉丝列表(ID)
     */
    public List<Long> getFollowerIds(Long userId) {
        return followMapper.selectFollowerIds(userId);
    }
    
    /**
     * 获取关注列表
     */
    public List<Follow> getFollowing(Long userId) {
        return followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId)
                .orderByDesc(Follow::getCreatedAt));
    }
    
    /**
     * 获取粉丝列表
     */
    public List<Follow> getFollowers(Long userId) {
        return followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowingId, userId)
                .orderByDesc(Follow::getCreatedAt));
    }
    
    /**
     * 获取关注数
     */
    public int getFollowingCount(Long userId) {
        return followMapper.countFollowing(userId);
    }
    
    /**
     * 获取粉丝数
     */
    public int getFollowerCount(Long userId) {
        return followMapper.countFollowers(userId);
    }
    
    /**
     * 检查是否互相关注
     */
    public boolean isMutualFollow(Long userId1, Long userId2) {
        return isFollowing(userId1, userId2) && isFollowing(userId2, userId1);
    }
}
