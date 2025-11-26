package com.example.av.controller;

import com.example.av.entity.CallRoom;
import com.example.av.service.CallRoomService;
import com.example.av.service.SignalingService;
import com.example.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 通话控制器
 */
@RestController
@RequestMapping("/api/v1/call")
@RequiredArgsConstructor
public class CallController {
    
    private final CallRoomService callRoomService;
    private final SignalingService signalingService;
    
    /**
     * 发起通话
     */
    @PostMapping("/initiate")
    public Result<CallRoom> initiateCall(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Set<Long> targetUserIds,
            @RequestParam(defaultValue = "VIDEO") String callType) {
        
        String roomType = targetUserIds.size() == 1 ? CallRoom.ROOM_P2P : CallRoom.ROOM_GROUP;
        CallRoom room = callRoomService.createRoom(userId, targetUserIds, callType, roomType);
        
        // 发送呼叫邀请
        for (Long targetId : targetUserIds) {
            signalingService.sendCallInvite(room.getRoomId(), userId, targetId, callType);
        }
        
        return Result.success(room);
    }
    
    /**
     * 接听通话
     */
    @PostMapping("/accept/{roomId}")
    public Result<CallRoom> acceptCall(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String roomId) {
        
        callRoomService.acceptCall(roomId, userId);
        CallRoom room = callRoomService.getRoom(roomId);
        
        // 通知发起人
        signalingService.sendCallResponse(roomId, userId, room.getInitiatorId(), true);
        
        return Result.success(room);
    }
    
    /**
     * 拒绝通话
     */
    @PostMapping("/reject/{roomId}")
    public Result<Void> rejectCall(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String roomId) {
        
        CallRoom room = callRoomService.getRoom(roomId);
        if (room != null) {
            signalingService.sendCallResponse(roomId, userId, room.getInitiatorId(), false);
            callRoomService.endCall(roomId, userId);
        }
        
        return Result.success();
    }
    
    /**
     * 挂断通话
     */
    @PostMapping("/hangup/{roomId}")
    public Result<Void> hangupCall(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String roomId) {
        
        CallRoom room = callRoomService.getRoom(roomId);
        if (room != null) {
            // 通知所有参与者
            for (Long participantId : room.getParticipantIds()) {
                if (!participantId.equals(userId)) {
                    signalingService.sendCallEnd(roomId, userId, participantId, "hangup");
                }
            }
            callRoomService.endCall(roomId, userId);
        }
        
        return Result.success();
    }
    
    /**
     * 获取房间信息
     */
    @GetMapping("/room/{roomId}")
    public Result<CallRoom> getRoomInfo(@PathVariable String roomId) {
        CallRoom room = callRoomService.getRoom(roomId);
        return Result.success(room);
    }
    
    /**
     * 获取用户当前通话
     */
    @GetMapping("/current")
    public Result<CallRoom> getCurrentCall(@RequestHeader("X-User-Id") Long userId) {
        String roomId = callRoomService.getUserCurrentRoom(userId);
        if (roomId != null) {
            return Result.success(callRoomService.getRoom(roomId));
        }
        return Result.success(null);
    }
}
