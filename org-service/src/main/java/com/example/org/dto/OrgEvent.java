package com.example.org.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 组织变更事件 (发送至MQ)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgEvent implements Serializable {
    
    /** 事件类型 */
    private String event;
    
    /** 目标ID (部门ID或用户ID) */
    private Long targetId;
    
    /** 父部门ID */
    private Long parentId;
    
    /** 旧路径前缀 (用于DEPT_MOVED) */
    private String oldPathPrefix;
    
    /** 新路径前缀 (用于DEPT_MOVED) */
    private String newPathPrefix;
    
    /** 扩展信息 */
    private Map<String, Object> payload;
    
    /** 时间戳 */
    private LocalDateTime timestamp;
    
    // 事件类型常量
    public static final String DEPT_CREATED = "DEPT_CREATED";
    public static final String DEPT_UPDATED = "DEPT_UPDATED";
    public static final String DEPT_MOVED = "DEPT_MOVED";
    public static final String DEPT_DELETED = "DEPT_DELETED";
    public static final String MEMBER_ADDED = "MEMBER_ADDED";
    public static final String MEMBER_REMOVED = "MEMBER_REMOVED";
    public static final String MEMBER_MOVED = "MEMBER_MOVED";
    
    public OrgEvent(String event, Long targetId) {
        this.event = event;
        this.targetId = targetId;
        this.timestamp = LocalDateTime.now();
    }
    
    public OrgEvent(String event, Long targetId, String oldPathPrefix, String newPathPrefix) {
        this.event = event;
        this.targetId = targetId;
        this.oldPathPrefix = oldPathPrefix;
        this.newPathPrefix = newPathPrefix;
        this.timestamp = LocalDateTime.now();
    }
}
