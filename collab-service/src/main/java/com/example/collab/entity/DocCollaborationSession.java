package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档协同会话（实时协同）
 */
@Data
@TableName("doc_collaboration_sessions")
public class DocCollaborationSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文档ID
     */
    private Long docId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话Token
     */
    private String sessionToken;

    /**
     * WebSocket连接ID
     */
    private String connectionId;

    /**
     * 处理节点
     */
    private String nodeId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户光标颜色
     */
    private String userColor;

    /**
     * 光标位置 (JSON)
     */
    private String cursorPosition;

    /**
     * 选区范围 (JSON)
     */
    private String selectionRange;

    /**
     * 可视区域 (JSON)
     */
    private String viewport;

    /**
     * 是否活跃
     */
    private Boolean isActive;

    /**
     * 是否正在编辑
     */
    private Boolean isEditing;

    /**
     * 最后活动时间
     */
    private LocalDateTime lastActivityAt;

    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeatAt;

    /**
     * 加入时间
     */
    private LocalDateTime joinedAt;

    /**
     * 离开时间
     */
    private LocalDateTime leftAt;
}
