package com.example.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文件转码任务实体
 * 对应 file_db.file_transcode_tasks 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "file_transcode_tasks", autoResultMap = true)
public class FileTranscodeTask {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String fileHash;
    
    private Long userId;
    
    /** 源格式 */
    private String sourceFormat;
    
    /** 目标格式 */
    private String targetFormat;
    
    /** 质量: high, medium, low */
    private String quality;
    
    /** 转码参数 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> parameters;
    
    /** 输出文件哈希 */
    private String outputHash;
    
    /** 输出文件路径 */
    private String outputKey;
    
    /** 状态: 0-待处理, 1-处理中, 2-成功, 3-失败 */
    private Integer status;
    
    /** 进度百分比 */
    private Integer progress;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 执行Worker ID */
    private String workerId;
    
    private LocalDateTime startedAt;
    
    private LocalDateTime completedAt;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 状态常量
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROCESSING = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 3;
}
