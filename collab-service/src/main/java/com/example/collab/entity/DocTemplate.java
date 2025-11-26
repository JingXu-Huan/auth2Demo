package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 文档模板实体
 * 对应 doc_db.doc_templates 表
 */
@Data
@Accessors(chain = true)
@TableName("doc_templates")
public class DocTemplate {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 模板名称 */
    private String name;
    
    /** 模板描述 */
    private String description;
    
    /** 分类 */
    private String category;
    
    /** 文档类型: text, spreadsheet, slide, canvas */
    private String docType;
    
    /** 内容(小文档) */
    private byte[] content;
    
    /** 内容URL(大文档) */
    private String contentUrl;
    
    /** 预览图 */
    private String previewImage;
    
    /** 使用次数 */
    private Integer useCount;
    
    /** 是否公开 */
    private Boolean isPublic;
    
    /** 创建者ID */
    private Long createdBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
