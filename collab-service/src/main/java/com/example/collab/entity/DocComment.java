package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 文档评论实体
 * 对应 doc_db.doc_comments 表
 */
@Data
@Accessors(chain = true)
@TableName("doc_comments")
public class DocComment {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long docId;
    
    /** 父评论ID(回复) */
    private Long parentId;
    
    private Long userId;
    
    private String userName;
    
    /** 评论内容 */
    private String content;
    
    /** 锚点起始位置 */
    private Integer anchorStart;
    
    /** 锚点结束位置 */
    private Integer anchorEnd;
    
    /** 锚点文本快照 */
    private String anchorText;
    
    /** 点赞数 */
    private Integer likeCount;
    
    /** 回复数 */
    private Integer replyCount;
    
    /** 是否已解决 */
    private Boolean isResolved;
    
    /** 是否置顶 */
    private Boolean isPinned;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    private LocalDateTime deletedAt;
}
