package com.example.av.service;

import com.example.av.websocket.SignalingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * WebRTC信令服务
 * 负责在通话参与者之间转发SDP/ICE等信令
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignalingService {
    
    private final SignalingHandler signalingHandler;
    
    /**
     * 转发Offer信令
     */
    public void forwardOffer(String roomId, Long fromUserId, Long toUserId, String sdp) {
        Map<String, Object> message = Map.of(
                "type", "offer",
                "roomId", roomId,
                "from", fromUserId,
                "sdp", sdp
        );
        signalingHandler.sendToUser(toUserId, message);
        log.debug("转发Offer: {} -> {}", fromUserId, toUserId);
    }
    
    /**
     * 转发Answer信令
     */
    public void forwardAnswer(String roomId, Long fromUserId, Long toUserId, String sdp) {
        Map<String, Object> message = Map.of(
                "type", "answer",
                "roomId", roomId,
                "from", fromUserId,
                "sdp", sdp
        );
        signalingHandler.sendToUser(toUserId, message);
        log.debug("转发Answer: {} -> {}", fromUserId, toUserId);
    }
    
    /**
     * 转发ICE候选者
     */
    public void forwardIceCandidate(String roomId, Long fromUserId, Long toUserId, String candidate) {
        Map<String, Object> message = Map.of(
                "type", "ice-candidate",
                "roomId", roomId,
                "from", fromUserId,
                "candidate", candidate
        );
        signalingHandler.sendToUser(toUserId, message);
        log.debug("转发ICE: {} -> {}", fromUserId, toUserId);
    }
    
    /**
     * 发送呼叫邀请
     */
    public void sendCallInvite(String roomId, Long fromUserId, Long toUserId, String callType) {
        Map<String, Object> message = Map.of(
                "type", "call-invite",
                "roomId", roomId,
                "from", fromUserId,
                "callType", callType
        );
        signalingHandler.sendToUser(toUserId, message);
        log.info("发送呼叫邀请: {} -> {}, type={}", fromUserId, toUserId, callType);
    }
    
    /**
     * 发送呼叫响应
     */
    public void sendCallResponse(String roomId, Long fromUserId, Long toUserId, boolean accepted) {
        Map<String, Object> message = Map.of(
                "type", "call-response",
                "roomId", roomId,
                "from", fromUserId,
                "accepted", accepted
        );
        signalingHandler.sendToUser(toUserId, message);
        log.info("发送呼叫响应: {} -> {}, accepted={}", fromUserId, toUserId, accepted);
    }
    
    /**
     * 发送通话结束通知
     */
    public void sendCallEnd(String roomId, Long fromUserId, Long toUserId, String reason) {
        Map<String, Object> message = Map.of(
                "type", "call-end",
                "roomId", roomId,
                "from", fromUserId,
                "reason", reason
        );
        signalingHandler.sendToUser(toUserId, message);
        log.info("发送通话结束: {} -> {}, reason={}", fromUserId, toUserId, reason);
    }
}
