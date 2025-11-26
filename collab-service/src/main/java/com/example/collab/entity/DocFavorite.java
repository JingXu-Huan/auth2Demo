package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 文档收藏实体
 * 对应 doc_db.doc_favorites 表
 */
@Data
@Accessors(chain = true)
@TableName("doc_favorites")
public class DocFavorite {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long docId;
    
    /** 收藏夹名称 */
    private String folderName;
    
    /** 备注 */
    private String notes;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
