package org.example.imgroupserver.service;

import com.example.domain.model.ConversationNode;
import com.example.domain.model.GroupNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.mapper.ConversationNodeMapper;
import org.example.imgroupserver.mapper.GroupNodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 会话服务：负责管理单聊 / 群聊的 Conversation 节点和 IN_CHAT 关系
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationNodeMapper conversationMapper;
    private final GroupNodeMapper groupMapper;
    private final FriendService friendService;

    /**
     * 创建或获取单聊会话
     */
    @Transactional
    public Map<String, Object> createOrGetP2PConversation(Long userAId, Long userBId) {
        if (userAId == null || userBId == null) {
            throw new IllegalArgumentException("userAId 和 userBId 不能为空");
        }

        Long minId = Math.min(userAId, userBId);
        Long maxId = Math.max(userAId, userBId);
        String conversationId = minId + "-" + maxId;

        ConversationNode conversation = conversationMapper
                .findByConversationId(conversationId)
                .orElseGet(() -> {
                    ConversationNode node = new ConversationNode();
                    node.setConversationId(conversationId);
                    node.setType("p2p");
                    node.setCreatedAt(LocalDateTime.now());
                    node.setUpdatedAt(LocalDateTime.now());
                    return conversationMapper.save(node);
                });

        // 确保用户节点存在
        friendService.ensureUserNodeExists(userAId);
        friendService.ensureUserNodeExists(userBId);

        // 建立 IN_CHAT 关系
        conversationMapper.addMember(conversationId, userAId);
        conversationMapper.addMember(conversationId, userBId);

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversation.getConversationId());
        result.put("type", conversation.getType());
        return result;
    }

    /**
     * 创建或获取群聊会话
     * conversationId 规则："GROUP:" + groupId
     */
    @Transactional
    public Map<String, Object> createOrGetGroupConversation(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            throw new IllegalArgumentException("groupId 不能为空");
        }

        GroupNode group = groupMapper.findByGroupId(groupId)
                .orElseThrow(() -> new NoSuchElementException("群组不存在: " + groupId));

        String conversationId = "GROUP:" + groupId;

        ConversationNode conversation = conversationMapper
                .findByConversationId(conversationId)
                .orElseGet(() -> {
                    ConversationNode node = new ConversationNode();
                    node.setConversationId(conversationId);
                    node.setType("group");
                    node.setName(group.getName());
                    node.setAvatar(group.getAvatar());
                    node.setCreatedAt(LocalDateTime.now());
                    node.setUpdatedAt(LocalDateTime.now());
                    return conversationMapper.save(node);
                });

        // 为所有群成员建立 IN_CHAT 关系
        List<Long> memberUserIds = groupMapper.findMemberUserIds(groupId);
        for (Long userId : memberUserIds) {
            try {
                friendService.ensureUserNodeExists(userId);
                conversationMapper.addMember(conversationId, userId);
            } catch (Exception e) {
                log.warn("为用户建立 IN_CHAT 关系失败: userId={}, groupId={}", userId, groupId, e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversation.getConversationId());
        result.put("type", conversation.getType());
        result.put("groupId", groupId);
        return result;
    }

    /**
     * 获取会话的所有参与用户ID
     */
    public List<Long> getConversationMembers(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("conversationId 不能为空");
        }
        return conversationMapper.findMemberUserIds(conversationId);
    }
}
