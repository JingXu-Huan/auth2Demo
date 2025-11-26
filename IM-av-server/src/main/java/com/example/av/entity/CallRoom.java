package com.example.av.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 通话房间
 */
@Data
@Accessors(chain = true)
public class CallRoom {
    
    /** 房间ID */
    private String roomId;
    
    /** 通话类型: AUDIO, VIDEO */
    private String callType;
    
    /** 房间类型: P2P, GROUP */
    private String roomType;
    
    /** 发起人ID */
    private Long initiatorId;
    
    /** 参与者ID列表 */
    private Set<Long> participantIds = new HashSet<>();
    
    /** 房间状态: WAITING, ACTIVE, ENDED */
    private String status;
    
    /** 创建时间 */
    private LocalDateTime createdAt;
    
    /** 开始时间 */
    private LocalDateTime startedAt;
    
    /** 结束时间 */
    private LocalDateTime endedAt;
    
    // 状态常量
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ENDED = "ENDED";
    
    // 通话类型
    public static final String TYPE_AUDIO = "AUDIO";
    public static final String TYPE_VIDEO = "VIDEO";
    
    // 房间类型
    public static final String ROOM_P2P = "P2P";
    public static final String ROOM_GROUP = "GROUP";
}
