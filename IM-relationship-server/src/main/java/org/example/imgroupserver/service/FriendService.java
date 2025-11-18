package org.example.imgroupserver.service;

import com.example.domain.dto.AddFriendRequest;
import com.example.domain.dto.FriendDTO;
import com.example.domain.model.UserNode;
import com.example.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.feign.UserServiceClient;
import org.example.imgroupserver.mapper.FriendMapper;
import org.example.imgroupserver.mapper.UserNodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Junjie
 * @version 1.0
 * @date 25-11-18
 * 好友服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {
    
    private final FriendMapper friendMapper;
    private final UserNodeMapper userMapper;
    private final UserServiceClient userServiceClient;
    
    /**
     * 发送好友请求
     */
    @Transactional
    public Long sendFriendRequest(AddFriendRequest request) {
        // 检查是否已经是好友
        Boolean isFriend = friendMapper.isFriend(request.getFromUserId(), request.getToUserId());
        if (Boolean.TRUE.equals(isFriend)) {
            throw new IllegalStateException("已经是好友关系");
        }
        
        // 检查是否已发送请求
        Boolean hasRequested = friendMapper.hasRequested(request.getFromUserId(), request.getToUserId());
        if (Boolean.TRUE.equals(hasRequested)) {
            throw new IllegalStateException("已发送好友请求，请等待对方处理");
        }
        
        // 确保两个用户节点都存在
        ensureUserNodeExists(request.getFromUserId());
        ensureUserNodeExists(request.getToUserId());
        
        // 发送好友请求
        Long requestId = friendMapper.sendFriendRequest(
            request.getFromUserId(), 
            request.getToUserId(),
            request.getMessage()
        );
        
        log.info("发送好友请求成功: {} -> {}, requestId: {}", 
                request.getFromUserId(), request.getToUserId(), requestId);
        return requestId;
    }
    
    /**
     * 接受好友请求
     */
    @Transactional
    public void acceptFriendRequest(Long fromUserId, Long toUserId) {
        friendMapper.acceptFriendRequest(fromUserId, toUserId);
        log.info("接受好友请求成功: {} -> {}", fromUserId, toUserId);
    }
    
    /**
     * 拒绝好友请求
     */
    @Transactional
    public void rejectFriendRequest(Long fromUserId, Long toUserId) {
        friendMapper.rejectFriendRequest(fromUserId, toUserId);
        log.info("拒绝好友请求成功: {} -> {}", fromUserId, toUserId);
    }
    
    /**
     * 获取收到的好友请求列表
     */
    @SuppressWarnings("unchecked")
    public List<com.example.domain.dto.FriendRequestDTO> getReceivedRequests(Long userId) {
        List<Object> queryResult = friendMapper.getReceivedRequests(userId);
        
        if (queryResult.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 提取请求列表
        List<Map<String, Object>> requestsMap = new ArrayList<>();
        if (queryResult.get(0) instanceof org.neo4j.driver.Value) {
            org.neo4j.driver.Value value = (org.neo4j.driver.Value) queryResult.get(0);
            List<Object> innerList = value.asList(v -> v.asMap());
            for (Object item : innerList) {
                if (item instanceof Map) {
                    requestsMap.add((Map<String, Object>) item);
                }
            }
        }
        
        // 转换为 DTO
        return requestsMap.stream()
                .map(this::mapToFriendRequestDTO)
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取好友列表
     */
    @SuppressWarnings("unchecked")
    public List<FriendDTO> getFriends(Long userId) {
        List<Object> queryResult = friendMapper.findFriends(userId);
        
        if (queryResult.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 提取好友列表
        List<Map<String, Object>> friendsMap = new ArrayList<>();
        if (queryResult.get(0) instanceof org.neo4j.driver.Value) {
            org.neo4j.driver.Value value = (org.neo4j.driver.Value) queryResult.get(0);
            List<Object> innerList = value.asList(v -> v.asMap());
            for (Object item : innerList) {
                if (item instanceof Map) {
                    friendsMap.add((Map<String, Object>) item);
                }
            }
        }
        
        // 转换为 DTO
        return friendsMap.stream()
                .map(this::mapToFriendDTO)
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
    
    /**
     * 删除好友
     */
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        friendMapper.deleteFriendship(userId, friendId);
        log.info("删除好友成功: {} -> {}", userId, friendId);
    }
    
    /**
     * 搜索好友
     */
    @SuppressWarnings("unchecked")
    public List<FriendDTO> searchFriends(Long userId, String keyword, int limit) {
        List<Object> queryResult = friendMapper.searchFriends(userId, keyword, limit);
        
        if (queryResult.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 提取好友列表
        List<Map<String, Object>> friendsMap = new ArrayList<>();
        if (queryResult.get(0) instanceof org.neo4j.driver.Value) {
            org.neo4j.driver.Value value = (org.neo4j.driver.Value) queryResult.get(0);
            List<Object> innerList = value.asList(v -> v.asMap());
            for (Object item : innerList) {
                if (item instanceof Map) {
                    friendsMap.add((Map<String, Object>) item);
                }
            }
        }
        
        // 转换为 DTO
        return friendsMap.stream()
                .map(this::mapToFriendDTO)
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
    
    /**
     * 转换为 FriendDTO
     */
    private FriendDTO mapToFriendDTO(Map<String, Object> map) {
        try {
            FriendDTO dto = new FriendDTO();
            dto.setUserId(map.get("userId") != null ? ((Number) map.get("userId")).longValue() : null);
            dto.setNickname((String) map.get("nickname"));
            dto.setAvatar((String) map.get("avatar"));
            dto.setStatus((String) map.get("status"));
            dto.setCreatedAt((String) map.get("createdAt"));
            return dto;
        } catch (Exception e) {
            log.error("转换好友数据失败: {}", map, e);
            return null;
        }
    }
    
    /**
     * 转换为 FriendRequestDTO
     */
    private com.example.domain.dto.FriendRequestDTO mapToFriendRequestDTO(Map<String, Object> map) {
        try {
            com.example.domain.dto.FriendRequestDTO dto = new com.example.domain.dto.FriendRequestDTO();
            dto.setRequestId(map.get("requestId") != null ? ((Number) map.get("requestId")).longValue() : null);
            dto.setFromUserId(map.get("fromUserId") != null ? ((Number) map.get("fromUserId")).longValue() : null);
            dto.setFromNickname((String) map.get("fromNickname"));
            dto.setFromAvatar((String) map.get("fromAvatar"));
            dto.setToUserId(map.get("toUserId") != null ? ((Number) map.get("toUserId")).longValue() : null);
            dto.setMessage((String) map.get("message"));
            dto.setStatus((String) map.get("status"));
            dto.setCreatedAt((String) map.get("createdAt"));
            return dto;
        } catch (Exception e) {
            log.error("转换好友请求数据失败: {}", map, e);
            return null;
        }
    }
    
    /**
     * 确保用户节点存在
     */
    private void ensureUserNodeExists(Long userId) {
        Long count = userMapper.countByUserId(userId);
        
        UserNode user = null;
        if (count != null && count > 0) {
            user = userMapper.findByUserId(userId).orElse(null);
        }
        
        if (user == null) {
            user = new UserNode();
            user.setUserId(userId);
        }
        
        // 从 User-server 获取真实用户信息并更新
        try {
            Result<Map<String, Object>> userResult = userServiceClient.getUserById(userId);
            if (userResult != null && userResult.getData() != null) {
                Map<String, Object> userData = userResult.getData();
                String username = (String) userData.getOrDefault("username", "用户" + userId);
                user.setNickname(username);
                log.info("从 User-server 获取用户信息成功: userId={}, username={}", userId, username);
            } else {
                user.setNickname("用户" + userId);
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败, userId={}, 使用默认昵称", userId, e);
            user.setNickname("用户" + userId);
        }
        
        user.setStatus("ONLINE");
        userMapper.save(user);
    }
}
