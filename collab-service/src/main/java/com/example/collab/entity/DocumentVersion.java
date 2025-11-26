package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文档版本实体
 * 对应 doc_db.document_versions 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "document_versions", autoResultMap = true)
public class DocumentVersion {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long docId;
    
    private Long versionNumber;
    
    /** 版本标题 */
    private String title;
    
    /** 版本说明 */
    private String description;
    
    /** 内容(小文档) */
    private byte[] content;
    
    /** 内容URL(大文档) */
    private String contentUrl;
    
    /** 内容哈希 */
    private String contentHash;
    
    /** 内容大小 */
    private Long contentSize;
    
    /** Yjs快照 */
    private byte[] yjsSnapshot;
    
    /** 作者ID */
    private Long authorId;
    
    /** 变更摘要 */
    private String changeSummary;
    
    /** 差异数据 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> diffData;
    
    /** 是否主要版本 */
    private Boolean isMajor;
    
    /** 是否已发布 */
    private Boolean isPublished;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
