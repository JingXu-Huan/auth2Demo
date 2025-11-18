package org.example.imgroupserver.service;

import com.example.domain.dto.AddMembersRequest;
import com.example.domain.dto.CreateGroupRequest;
import com.example.domain.dto.UpdateGroupRequest;
import com.example.domain.model.GroupNode;
import com.example.domain.model.UserNode;
import com.example.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.domain.dto.GroupMemberDTO;
import org.example.imgroupserver.feign.UserServiceClient;
import org.example.imgroupserver.mapper.GroupNodeMapper;
import org.example.imgroupserver.mapper.UserNodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.imgroupserver.util.SystemEventPublisher;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/*
  @author Junjie
  @version 1.0
  @date 2025-11-18
  Neo4j群组服务,其主要功能为创建群组,删除群组,管理群组等等.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class Neo4jGroupService {

    private final GroupNodeMapper groupMapper;
    private final UserNodeMapper userMapper;
    private final UserServiceClient userServiceClient;
    private final SystemEventPublisher eventPublisher;

    @Transactional
    public GroupNode createGroup(CreateGroupRequest request) {
        if (request.getOwnerId() == null) {
            throw new IllegalArgumentException("群主ID不能为空");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("群组名称不能为空");
        }

        String groupId = generateGroupId();

        GroupNode group = new GroupNode();
        group.setGroupId(groupId);
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setAvatar(request.getAvatar());
        group.setOwnerId(request.getOwnerId());
        group.setMaxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 500);
        group.setJoinType(request.getJoinType() != null ? request.getJoinType().name() : "FREE");
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());

        GroupNode savedGroup = groupMapper.save(group);

        ensureUserNodeExists(request.getOwnerId());
        groupMapper.addMember(savedGroup.getGroupId(), request.getOwnerId(), "OWNER");

        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            List<Long> distinctIds = request.getMemberIds().stream()
                    .filter(id -> !Objects.equals(id, request.getOwnerId()))
                    .distinct()
                    .collect(Collectors.toList());
            for (Long userId : distinctIds) {
                ensureUserNodeExists(userId);
                groupMapper.addMember(savedGroup.getGroupId(), userId, "MEMBER");
            }
        }

        return savedGroup;
    }

    public GroupNode getGroup(String groupId) {
        GroupNode group = groupMapper.findByGroupId(groupId)
                .orElseThrow(() -> new NoSuchElementException("群组不存在: " + groupId));
        // 设置成员数量
        Long count = groupMapper.countMembers(groupId);
        group.setMemberCount(count != null ? count.intValue() : 0);
        return group;
    }

    public GroupNode updateGroup(String groupId, UpdateGroupRequest request) {
        GroupNode group = getGroup(groupId);
        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        if (request.getAvatar() != null) {
            group.setAvatar(request.getAvatar());
        }
        if (request.getAnnouncement() != null) {
            group.setAnnouncement(request.getAnnouncement());
        }
        if (request.getJoinType() != null) {
            group.setJoinType(request.getJoinType().name());
        }
        group.setUpdatedAt(LocalDateTime.now());
        return groupMapper.save(group);
    }

    @Transactional
    public void dissolveGroup(String groupId) {
        if (!groupMapper.findByGroupId(groupId).isPresent()) {
            throw new NoSuchElementException("群组不存在: " + groupId);
        }

        // 发送群解散事件通知
        try {
            eventPublisher.publishGroupDisbandedEvent(groupId);
        } catch (Exception e) {
            log.error("发送群解散事件失败", e);
        }

        groupMapper.deleteGroupAndRelationships(groupId);
    }

    @Transactional
    public Map<String, Object> addMembers(String groupId, AddMembersRequest request) {
        GroupNode group = getGroup(groupId);
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new IllegalArgumentException("userIds 不能为空");
        }

        List<Long> failedUsers = new ArrayList<>();
        int addedCount = 0;

        for (Long userId : request.getUserIds()) {
            if (groupMapper.isMember(groupId, userId)) {
                failedUsers.add(userId);
                continue;
            }
            
            Long memberCount = groupMapper.countMembers(groupId);
            if (memberCount >= group.getMaxMembers()) {
                failedUsers.add(userId);
                continue;
            }
            
            ensureUserNodeExists(userId);
            groupMapper.addMember(groupId, userId, "MEMBER");
            addedCount++;
        }

        Long currentMemberCount = groupMapper.countMembers(groupId);

        // 发送系统事件通知
        if (addedCount > 0) {
            try {
                // 获取新添加成员的信息
                List<Map<String, Object>> addedMembersList = new ArrayList<>();
                for (Long userId : request.getUserIds()) {
                    if (!failedUsers.contains(userId)) {
                        UserNode user = userMapper.findByUserId(userId).orElse(null);
                        if (user != null) {
                            Map<String, Object> memberInfo = new HashMap<>();
                            memberInfo.put("user_id", userId.toString());
                            memberInfo.put("name", user.getNickname() != null ? user.getNickname() : "用户" + userId);
                            addedMembersList.add(memberInfo);
                        }
                    }
                }

                // 获取邀请人信息
                String operatorName = "管理员";
                if (request.getInviterId() != null) {
                    UserNode inviter = userMapper.findByUserId(request.getInviterId()).orElse(null);
                    if (inviter != null) {
                        operatorName = inviter.getNickname() != null ? inviter.getNickname() : "用户" + request.getInviterId();
                    }
                }

                eventPublisher.publishMemberAddedEvent(groupId, operatorName, addedMembersList);
            } catch (Exception e) {
                log.error("发送成员添加事件失败", e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("addedCount", addedCount);
        result.put("failedUsers", failedUsers);
        result.put("currentMemberCount", currentMemberCount);
        return result;
    }

    @Transactional
    public Map<String, Object> removeMember(String groupId, Long userId) {
        getGroup(groupId);
        String role = groupMapper.getMemberRole(groupId, userId);
        if (role == null) {
            throw new NoSuchElementException("成员不存在于群组中");
        }
        if ("OWNER".equals(role)) {
            throw new IllegalStateException("不能直接移除群主，请先转让或解散群组");
        }

        groupMapper.removeMember(groupId, userId);
        Long count = groupMapper.countMembers(groupId);

        // 发送系统事件通知（被踢出）
        try {
            UserNode removedUser = userMapper.findByUserId(userId).orElse(null);
            if (removedUser != null) {
                List<Map<String, Object>> removedMembersList = new ArrayList<>();
                Map<String, Object> memberInfo = new HashMap<>();
                memberInfo.put("user_id", userId.toString());
                memberInfo.put("name", removedUser.getNickname() != null ? removedUser.getNickname() : "用户" + userId);
                removedMembersList.add(memberInfo);

                eventPublisher.publishMemberRemovedEvent(groupId, "管理员", removedMembersList);
            }
        } catch (Exception e) {
            log.error("发送成员移除事件失败", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("removedUserId", userId);
        result.put("currentMemberCount", count);
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> listMembers(String groupId, String role, int page, int size) {
        log.info("开始查询群组成员, groupId={}", groupId);
        
        // 先更新所有成员的用户信息
        List<Long> memberUserIds = groupMapper.findMemberUserIds(groupId);
        for (Long userId : memberUserIds) {
            try {
                ensureUserNodeExists(userId);
            } catch (Exception e) {
                log.error("更新用户信息失败: userId={}", userId, e);
            }
        }
        
        List<Object> queryResult = groupMapper.findMembersWithRoleByGroupId(groupId);
        Long total = groupMapper.countMembers(groupId);

        log.info("查询结果大小: {}, 成员总数: {}", queryResult.size(), total);
        log.info("查询结果内容: {}", queryResult);

        // 提取 members 列表 - 处理 Neo4j ListValue 类型
        List<Map<String, Object>> allMembersMap = new ArrayList<>();
        if (!queryResult.isEmpty()) {
            Object firstElement = queryResult.get(0);
            log.info("第一个元素类型: {}, 内容: {}", firstElement.getClass().getName(), firstElement);
            
            try {
                // 尝试使用 Neo4j Value 的 asList 方法
                if (firstElement instanceof org.neo4j.driver.Value) {
                    org.neo4j.driver.Value value = (org.neo4j.driver.Value) firstElement;
                    List<Object> innerList = value.asList(v -> v.asMap());
                    log.info("使用 Neo4j Value.asList 提取, 大小: {}", innerList.size());
                    
                    for (Object item : innerList) {
                        if (item instanceof Map) {
                            allMembersMap.add((Map<String, Object>) item);
                        }
                    }
                } else if (firstElement instanceof List) {
                    // 标准 Java List
                    List<?> innerList = (List<?>) firstElement;
                    log.info("标准 List, 大小: {}", innerList.size());
                    for (Object item : innerList) {
                        if (item instanceof Map) {
                            allMembersMap.add((Map<String, Object>) item);
                        }
                    }
                } else {
                    log.warn("未知类型，尝试 toString: {}", firstElement);
                }
                
                log.info("成功提取 {} 个成员", allMembersMap.size());
            } catch (Exception e) {
                log.error("提取成员列表失败", e);
            }
        } else {
            log.warn("查询结果为空!");
        }

        log.info("最终查询到 {} 条成员记录", allMembersMap.size());

        // 转换为 DTO，过滤掉 null
        List<GroupMemberDTO> allMembers = allMembersMap.stream()
                .map(this::mapToGroupMemberDTO)
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        log.debug("转换后有 {} 个有效成员", allMembers.size());

        // 如果指定了角色过滤
        if (role != null && !"ALL".equalsIgnoreCase(role)) {
            allMembers = allMembers.stream()
                    .filter(m -> role.equalsIgnoreCase(m.getRole()))
                    .collect(Collectors.toList());
        }

        int skip = (page - 1) * size;
        List<GroupMemberDTO> pagedMembers = allMembers.stream()
                .skip(skip)
                .limit(size)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("members", pagedMembers);
        return result;
    }
    
    /**
     * 将 Map 转换为 GroupMemberDTO
     */
    private GroupMemberDTO mapToGroupMemberDTO(Map<String, Object> map) {
        try {
            log.debug("转换记录: {}", map);
            
            GroupMemberDTO dto = new GroupMemberDTO();
            dto.setUserId(map.get("userId") != null ? ((Number) map.get("userId")).longValue() : null);
            dto.setNickname((String) map.get("nickname"));
            dto.setAvatar((String) map.get("avatar"));
            dto.setStatus((String) map.get("status"));
            dto.setRole((String) map.get("role"));
            dto.setJoinedAt((String) map.get("joinedAt"));
            dto.setMuted(map.get("muted") != null ? (Boolean) map.get("muted") : false);
            dto.setGroupNickname((String) map.get("groupNickname"));
            
            log.debug("转换后的 DTO: userId={}, nickname={}, role={}", dto.getUserId(), dto.getNickname(), dto.getRole());
            return dto;
        } catch (Exception e) {
            log.error("转换成员数据失败: {}", map, e);
            return null;
        }
    }

    @Transactional
    public Map<String, Object> setAdmin(String groupId, Long userId, String action) {
        getGroup(groupId);
        String currentRole = groupMapper.getMemberRole(groupId, userId);
        if (currentRole == null) {
            throw new NoSuchElementException("成员不存在于群组中");
        }
        if ("OWNER".equals(currentRole)) {
            throw new IllegalStateException("群主角色无法更改");
        }
        
        String newRole;
        if ("ADD".equalsIgnoreCase(action)) {
            newRole = "ADMIN";
        } else if ("REMOVE".equalsIgnoreCase(action)) {
            newRole = "MEMBER";
        } else {
            throw new IllegalArgumentException("action 只能为 ADD 或 REMOVE");
        }
        
        groupMapper.updateMemberRole(groupId, userId, newRole);

        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("userId", userId);
        result.put("role", newRole);
        return result;
    }

    @Transactional
    public Map<String, Object> leaveGroup(String groupId, Long userId) {
        getGroup(groupId);
        String role = groupMapper.getMemberRole(groupId, userId);
        if (role == null) {
            throw new NoSuchElementException("用户不在群组中");
        }
        if ("OWNER".equals(role)) {
            throw new IllegalStateException("群主不能直接退群，请先转让群主或解散群组");
        }

        groupMapper.removeMember(groupId, userId);
        Long count = groupMapper.countMembers(groupId);

        // 发送系统事件通知（成员主动退出）
        try {
            UserNode leftUser = userMapper.findByUserId(userId).orElse(null);
            if (leftUser != null) {
                List<Map<String, Object>> leftMembersList = new ArrayList<>();
                Map<String, Object> memberInfo = new HashMap<>();
                memberInfo.put("user_id", userId.toString());
                memberInfo.put("name", leftUser.getNickname() != null ? leftUser.getNickname() : "用户" + userId);
                leftMembersList.add(memberInfo);

                // 使用 member_removed 事件类型，但操作者是自己
                String operatorName = leftUser.getNickname() != null ? leftUser.getNickname() : "用户" + userId;
                
                // 发送成员移除事件，标记为主动退出
                eventPublisher.publishMemberRemovedEvent(groupId, operatorName, leftMembersList, true);
            }
        } catch (Exception e) {
            log.error("发送成员退出事件失败", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("leftUserId", userId);
        result.put("currentMemberCount", count);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<GroupMemberDTO> searchMembers(String groupId, String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Object> result = groupMapper.findMembersWithRoleByGroupId(groupId);
        
        // 提取 members 列表
        List<Map<String, Object>> allMembersMap = new ArrayList<>();
        if (!result.isEmpty() && result.get(0) instanceof List) {
            allMembersMap = (List<Map<String, Object>>) result.get(0);
        }
        
        return allMembersMap.stream()
                .map(this::mapToGroupMemberDTO)
                .filter(member -> member != null && member.getNickname() != null && 
                        member.getNickname().contains(keyword.trim()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getUserGroups(Long userId, int page, int size) {
        int skip = (page - 1) * size;
        List<GroupNode> groups = groupMapper.findGroupsByUserId(userId, skip, size);
        Long total = groupMapper.countUserGroups(userId);

        // 为每个群组设置成员数量
        for (GroupNode group : groups) {
            Long count = groupMapper.countMembers(group.getGroupId());
            group.setMemberCount(count != null ? count.intValue() : 0);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("groups", groups);
        return result;
    }

    @Transactional
    public Map<String, Object> transferOwnership(String groupId, Long currentOwnerId, Long newOwnerId) {
        GroupNode group = getGroup(groupId);

        if (!Objects.equals(group.getOwnerId(), currentOwnerId)) {
            throw new IllegalArgumentException("当前用户不是群主");
        }

        if (!groupMapper.isMember(groupId, newOwnerId)) {
            throw new NoSuchElementException("新群主不在群组中");
        }

        groupMapper.updateMemberRole(groupId, currentOwnerId, "MEMBER");
        groupMapper.updateMemberRole(groupId, newOwnerId, "OWNER");

        group.setOwnerId(newOwnerId);
        group.setUpdatedAt(LocalDateTime.now());
        groupMapper.save(group);

        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("oldOwnerId", currentOwnerId);
        result.put("newOwnerId", newOwnerId);
        return result;
    }

    private void ensureUserNodeExists(Long userId) {
        // 使用自定义查询检查用户是否存在，避免加载关系数据
        Long count = userMapper.countByUserId(userId);
        
        UserNode user = null;
        if (count != null && count > 0) {
            // 用户已存在，获取并更新
            user = userMapper.findByUserId(userId).orElse(null);
        }
        
        if (user == null) {
            user = new UserNode();
            user.setUserId(userId);
        }
        
        // 尝试从 User-server 获取真实用户信息并更新
        try {
            Result<Map<String, Object>> userResult = userServiceClient.getUserById(userId);
            if (userResult != null && userResult.getData() != null) {
                Map<String, Object> userData = userResult.getData();
                String username = (String) userData.getOrDefault("username", "用户" + userId);
                user.setNickname(username);
                log.info("从 User-server 获取用户信息成功: userId={}, username={}", userId, username);
            } else {
                user.setNickname("用户" + userId);
                log.warn("User-server 返回空数据, userId={}", userId);
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败, userId={}, 使用默认昵称", userId, e);
            user.setNickname("用户" + userId);
        }
        
        user.setStatus("ONLINE");
        userMapper.save(user);
    }

    private String generateGroupId() {
        return "group_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
