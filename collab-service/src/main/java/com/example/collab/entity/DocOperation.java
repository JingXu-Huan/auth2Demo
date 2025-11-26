package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文档操作日志实体 (CRDT操作流)
 * 对应 doc_db.doc_operations 分区表
 */
@Data
@Accessors(chain = true)
@TableName(value = "doc_operations", autoResultMap = true)
public class DocOperation {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long docId;
    
    private Long sessionId;
    
    private Long userId;
    
    /** 操作唯一ID */
    private String operationId;
    
    /** 父操作ID */
    private String parentOperationId;
    
    /** 操作类型: insert, delete, format, style */
    private String operationType;
    
    /** Yjs Update (二进制) */
    private byte[] yjsUpdate;
    
    /** 操作位置 */
    private Integer position;
    
    /** 影响长度 */
    private Integer length;
    
    /** 操作内容预览 */
    private String content;
    
    /** 向量时钟 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Long> vectorClock;
    
    /** Lamport时间戳 */
    private Long lamportTimestamp;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 操作类型常量
    public static final String OP_INSERT = "insert";
    public static final String OP_DELETE = "delete";
    public static final String OP_FORMAT = "format";
    public static final String OP_STYLE = "style";
}
