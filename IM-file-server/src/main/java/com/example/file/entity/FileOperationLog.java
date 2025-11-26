package com.example.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文件操作日志实体
 * 对应 file_db.file_operation_logs 分区表
 */
@Data
@Accessors(chain = true)
@TableName(value = "file_operation_logs", autoResultMap = true)
public class FileOperationLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long fileId;
    
    private String fileHash;
    
    /** 操作类型: upload, download, delete, share, rename */
    private String operation;
    
    /** 操作详情 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> details;
    
    private String ipAddress;
    
    private String userAgent;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 操作类型常量
    public static final String OP_UPLOAD = "upload";
    public static final String OP_DOWNLOAD = "download";
    public static final String OP_DELETE = "delete";
    public static final String OP_SHARE = "share";
    public static final String OP_RENAME = "rename";
}
