package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文档活动流实体
 * 对应 doc_db.doc_activities 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "doc_activities", autoResultMap = true)
public class DocActivity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long docId;
    
    private Long userId;
    
    /** 活动类型: view, edit, comment, share, export */
    private String activityType;
    
    /** 活动详情 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> details;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 活动类型常量
    public static final String TYPE_VIEW = "view";
    public static final String TYPE_EDIT = "edit";
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_SHARE = "share";
    public static final String TYPE_EXPORT = "export";
}
