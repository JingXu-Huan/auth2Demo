package com.example.av.service;

import com.example.av.entity.CallRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 通话房间服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallRoomService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String ROOM_KEY_PREFIX = "av:room:";
    private static final String USER_ROOM_KEY_PREFIX = "av:user:room:";
    private static final long ROOM_EXPIRE_HOURS = 2;
    
    /**
     * 创建通话房间
     */
    public CallRoom createRoom(Long initiatorId, Set<Long> targetIds, String callType, String roomType) {
        String roomId = UUID.randomUUID().toString().replace("-", "");
        
        CallRoom room = new CallRoom()
                .setRoomId(roomId)
                .setInitiatorId(initiatorId)
                .setCallType(callType)
                .setRoomType(roomType)
                .setStatus(CallRoom.STATUS_WAITING)
                .setCreatedAt(LocalDateTime.now());
        
        room.getParticipantIds().add(initiatorId);
        room.getParticipantIds().addAll(targetIds);
        
        // 存储房间信息
        String roomKey = ROOM_KEY_PREFIX + roomId;
        redisTemplate.opsForValue().set(roomKey, room, ROOM_EXPIRE_HOURS, TimeUnit.HOURS);
        
        // 记录用户当前房间
        for (Long userId : room.getParticipantIds()) {
            redisTemplate.opsForValue().set(USER_ROOM_KEY_PREFIX + userId, roomId, ROOM_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        log.info("创建通话房间: roomId={}, type={}, initiator={}", roomId, callType, initiatorId);
        return room;
    }
    
    /**
     * 获取房间
     */
    public CallRoom getRoom(String roomId) {
        String roomKey = ROOM_KEY_PREFIX + roomId;
        return (CallRoom) redisTemplate.opsForValue().get(roomKey);
    }
    
    /**
     * 获取用户当前房间
     */
    public String getUserCurrentRoom(Long userId) {
        return (String) redisTemplate.opsForValue().get(USER_ROOM_KEY_PREFIX + userId);
    }
    
    /**
     * 接听通话
     */
    public void acceptCall(String roomId, Long userId) {
        CallRoom room = getRoom(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }
        
        if (!room.getParticipantIds().contains(userId)) {
            throw new RuntimeException("无权加入此房间");
        }
        
        if (room.getStatus().equals(CallRoom.STATUS_WAITING)) {
            room.setStatus(CallRoom.STATUS_ACTIVE);
            room.setStartedAt(LocalDateTime.now());
            updateRoom(room);
        }
        
        log.info("用户接听通话: roomId={}, userId={}", roomId, userId);
    }
    
    /**
     * 拒绝/挂断通话
     */
    public void endCall(String roomId, Long userId) {
        CallRoom room = getRoom(roomId);
        if (room == null) {
            return;
        }
        
        room.setStatus(CallRoom.STATUS_ENDED);
        room.setEndedAt(LocalDateTime.now());
        updateRoom(room);
        
        // 清除用户房间关联
        for (Long participantId : room.getParticipantIds()) {
            redisTemplate.delete(USER_ROOM_KEY_PREFIX + participantId);
        }
        
        log.info("通话结束: roomId={}, endedBy={}", roomId, userId);
    }
    
    /**
     * 离开房间
     */
    public void leaveRoom(String roomId, Long userId) {
        CallRoom room = getRoom(roomId);
        if (room == null) {
            return;
        }
        
        room.getParticipantIds().remove(userId);
        redisTemplate.delete(USER_ROOM_KEY_PREFIX + userId);
        
        // 如果房间没人了或者P2P通话有人离开，结束房间
        if (room.getParticipantIds().isEmpty() || 
            (room.getRoomType().equals(CallRoom.ROOM_P2P) && room.getParticipantIds().size() < 2)) {
            endCall(roomId, userId);
        } else {
            updateRoom(room);
        }
        
        log.info("用户离开房间: roomId={}, userId={}", roomId, userId);
    }
    
    private void updateRoom(CallRoom room) {
        String roomKey = ROOM_KEY_PREFIX + room.getRoomId();
        redisTemplate.opsForValue().set(roomKey, room, ROOM_EXPIRE_HOURS, TimeUnit.HOURS);
    }
}
