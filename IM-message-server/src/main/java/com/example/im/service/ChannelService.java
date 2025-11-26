package com.example.im.service;

import com.example.im.client.UserClient;
import com.example.im.entity.Channel;
import com.example.im.entity.ChannelMember;
import com.example.im.mapper.ChannelMapper;
import com.example.im.mapper.ChannelMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 会话服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelMapper channelMapper;
    private final ChannelMemberMapper channelMemberMapper;
    private final SequenceService sequenceService;
    private final UserClient userClient;

    /**
     * 创建单聊会话
     */
    @Transactional
    public Channel createPrivateChannel(Long userId1, Long userId2) {
        log.info("开始创建单聊会话: userId1={}, userId2={}", userId1, userId2);
        
        // 创建会话
        Channel channel = new Channel();
        channel.setChannelType(1); // 单聊
        channel.setMemberCount(2);
        channel.setMaxMembers(2);
        channel.setStatus(1);
        channel.setCreatedAt(OffsetDateTime.now());
        channel.setUpdatedAt(OffsetDateTime.now());
        channelMapper.insert(channel);
        log.info("频道创建成功: channelId={}", channel.getId());

        // 初始化序号
        sequenceService.initSeqId(channel.getId(), 0L);

        // 添加成员 - 确保两个都添加
        log.info("添加第一个成员: channelId={}, userId={}", channel.getId(), userId1);
        addMember(channel.getId(), userId1, 1);
        
        log.info("添加第二个成员: channelId={}, userId={}", channel.getId(), userId2);
        addMember(channel.getId(), userId2, 1);

        // 验证成员数量
        List<Long> memberIds = channelMemberMapper.getMemberIds(channel.getId());
        log.info("创建单聊会话完成: channelId={}, users=[{}, {}], 实际成员数={}, 成员列表={}", 
                channel.getId(), userId1, userId2, memberIds.size(), memberIds);
        
        return channel;
    }

    /**
     * 创建群聊会话
     */
    @Transactional
    public Channel createGroupChannel(Long ownerId, String name, List<Long> memberIds) {
        // 创建会话
        Channel channel = new Channel();
        channel.setChannelType(2); // 群聊
        channel.setName(name);
        channel.setOwnerId(ownerId);
        channel.setMemberCount(memberIds.size() + 1);
        channel.setMaxMembers(500);
        channel.setJoinType(2); // 需审批
        channel.setStatus(1);
        channel.setCreatedAt(OffsetDateTime.now());
        channel.setUpdatedAt(OffsetDateTime.now());
        channelMapper.insert(channel);

        // 初始化序号
        sequenceService.initSeqId(channel.getId(), 0L);

        // 添加群主
        addMember(channel.getId(), ownerId, 3);

        // 添加成员
        for (Long memberId : memberIds) {
            if (!memberId.equals(ownerId)) {
                addMember(channel.getId(), memberId, 1);
            }
        }

        log.info("创建群聊会话: channelId={}, name={}, memberCount={}",
                channel.getId(), name, channel.getMemberCount());
        return channel;
    }

    /**
     * 添加成员（如果已存在则跳过）
     */
    public void addMember(Long channelId, Long userId, Integer role) {
        log.info("添加成员: channelId={}, userId={}, role={}", channelId, userId, role);
        
        // 检查成员是否已存在（包括已离开的）
        ChannelMember existing = channelMemberMapper.findMemberIncludeLeft(channelId, userId);
        if (existing != null) {
            log.info("成员已存在: channelId={}, userId={}, leftAt={}", channelId, userId, existing.getLeftAt());
            // 如果已离开，重新加入
            if (existing.getLeftAt() != null) {
                existing.setLeftAt(null);
                existing.setJoinedAt(OffsetDateTime.now());
                channelMemberMapper.updateById(existing);
                log.info("成员重新加入: channelId={}, userId={}", channelId, userId);
            }
            return;
        }
        
        ChannelMember member = new ChannelMember();
        member.setChannelId(channelId);
        member.setUserId(userId);
        member.setRole(role);
        member.setUnreadCount(0);
        member.setMentionCount(0);
        member.setPinned(false);
        member.setShowNickname(true);
        member.setJoinedAt(OffsetDateTime.now());
        channelMemberMapper.insert(member);
        log.info("成员添加成功: channelId={}, userId={}", channelId, userId);
    }

    /**
     * 获取会话成员数
     */
    public Integer getMemberCount(Long channelId) {
        return channelMapper.getMemberCount(channelId);
    }

    /**
     * 判断是否为小群
     */
    public boolean isSmallGroup(Long channelId) {
        Boolean result = channelMapper.isSmallGroup(channelId);
        return result != null && result;
    }

    /**
     * 获取会话所有成员ID
     */
    public List<Long> getMemberIds(Long channelId) {
        return channelMemberMapper.getMemberIds(channelId);
    }
    
    /**
     * 创建频道
     */
    @Transactional
    public Channel createChannel(Channel channel, Long userId) {
        channel.setOwnerId(userId);
        channel.setMemberCount(1);
        channel.setStatus(1);
        channel.setCreatedAt(OffsetDateTime.now());
        channel.setUpdatedAt(OffsetDateTime.now());
        channelMapper.insert(channel);
        
        // 初始化序号
        sequenceService.initSeqId(channel.getId(), 0L);
        
        // 添加创建者为管理员
        addMember(channel.getId(), userId, 3);
        
        log.info("创建频道: channelId={}, ownerId={}", channel.getId(), userId);
        return channel;
    }
    
    /**
     * 获取频道详情
     */
    public Channel getChannelById(Long channelId) {
        return channelMapper.selectById(channelId);
    }
    
    /**
     * 更新频道信息
     */
    @Transactional
    public Channel updateChannel(Long channelId, Channel channel) {
        Channel existing = channelMapper.selectById(channelId);
        if (existing == null) {
            throw new IllegalArgumentException("频道不存在");
        }
        existing.setName(channel.getName());
        existing.setAvatarUrl(channel.getAvatarUrl());
        existing.setDescription(channel.getDescription());
        existing.setUpdatedAt(OffsetDateTime.now());
        channelMapper.updateById(existing);
        return existing;
    }
    
    /**
     * 删除/解散频道
     */
    @Transactional
    public void deleteChannel(Long channelId, Long userId) {
        Channel channel = channelMapper.selectById(channelId);
        if (channel == null) {
            return;
        }
        if (!channel.getOwnerId().equals(userId)) {
            throw new IllegalStateException("只有群主可以解散频道");
        }
        channel.setStatus(0);
        channel.setUpdatedAt(OffsetDateTime.now());
        channelMapper.updateById(channel);
        log.info("解散频道: channelId={}", channelId);
    }
    
    /**
     * 获取用户的频道列表
     */
    public List<Channel> getUserChannels(Long userId) {
        List<Long> channelIds = channelMemberMapper.getChannelIdsByUser(userId);
        if (channelIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Channel> channels = channelMapper.selectBatchIds(channelIds);
        
        // 为私聊会话设置对方用户名
        for (Channel channel : channels) {
            if (channel.getChannelType() != null && channel.getChannelType() == 1) {
                // 私聊会话：获取对方用户ID和用户名
                List<Long> memberIds = channelMemberMapper.getMemberIds(channel.getId());
                Long targetUserId = memberIds.stream()
                        .filter(id -> !id.equals(userId))
                        .findFirst()
                        .orElse(null);
                
                if (targetUserId != null) {
                    channel.setTargetUserId(targetUserId);
                    // 获取对方用户名
                    String displayName = getUserDisplayName(targetUserId);
                    channel.setDisplayName(displayName);
                    // 如果没有设置 name，使用 displayName
                    if (channel.getName() == null || channel.getName().isEmpty()) {
                        channel.setName(displayName);
                    }
                }
            }
        }
        
        return channels;
    }
    
    /**
     * 获取用户显示名称
     */
    private String getUserDisplayName(Long userId) {
        try {
            java.util.Map<String, Object> result = userClient.getUserById(userId);
            if (result != null && result.get("data") != null) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> data = (java.util.Map<String, Object>) result.get("data");
                // 优先使用 nickname，其次 username
                String nickname = (String) data.get("nickname");
                if (nickname != null && !nickname.isEmpty()) {
                    return nickname;
                }
                String username = (String) data.get("username");
                if (username != null && !username.isEmpty()) {
                    return username;
                }
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败: userId={}, error={}", userId, e.getMessage());
        }
        return "用户" + userId;
    }
    
    /**
     * 批量添加成员
     */
    @Transactional
    public void addMembers(Long channelId, List<Long> memberIds, Long operatorId) {
        for (Long memberId : memberIds) {
            addMember(channelId, memberId, 1);
        }
        // 更新成员数
        Channel channel = channelMapper.selectById(channelId);
        channel.setMemberCount(channel.getMemberCount() + memberIds.size());
        channelMapper.updateById(channel);
        log.info("添加成员: channelId={}, count={}", channelId, memberIds.size());
    }
    
    /**
     * 移除成员
     */
    @Transactional
    public void removeMember(Long channelId, Long memberId, Long operatorId) {
        channelMemberMapper.leaveChannel(channelId, memberId);
        // 更新成员数
        Channel channel = channelMapper.selectById(channelId);
        channel.setMemberCount(Math.max(0, channel.getMemberCount() - 1));
        channelMapper.updateById(channel);
        log.info("移除成员: channelId={}, memberId={}", channelId, memberId);
    }
    
    /**
     * 获取频道成员列表
     */
    public List<ChannelMember> getChannelMembers(Long channelId) {
        return channelMemberMapper.getChannelMembers(channelId);
    }
    
    /**
     * 退出频道
     */
    @Transactional
    public void leaveChannel(Long channelId, Long userId) {
        removeMember(channelId, userId, userId);
        log.info("退出频道: channelId={}, userId={}", channelId, userId);
    }
    
    /**
     * 设置管理员
     */
    @Transactional
    public void setAdmin(Long channelId, Long memberId, boolean isAdmin, Long operatorId) {
        ChannelMember member = channelMemberMapper.getMember(channelId, memberId);
        if (member == null) {
            throw new IllegalArgumentException("成员不存在");
        }
        member.setRole(isAdmin ? 2 : 1);
        channelMemberMapper.updateById(member);
        log.info("设置管理员: channelId={}, memberId={}, isAdmin={}", channelId, memberId, isAdmin);
    }
    
    /**
     * 获取或创建私聊频道
     */
    @Transactional
    public Channel getOrCreatePrivateChannel(Long userId, Long targetUserId) {
        // 查找已存在的私聊频道
        Long channelId = channelMemberMapper.findPrivateChannelId(userId, targetUserId);
        if (channelId != null) {
            return channelMapper.selectById(channelId);
        }
        // 创建新的私聊频道
        return createPrivateChannel(userId, targetUserId);
    }
}
