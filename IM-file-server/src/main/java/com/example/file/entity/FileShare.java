package com.example.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件分享记录实体
 * 对应 file_db.file_shares 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "file_shares", autoResultMap = true)
public class FileShare {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long fileId;
    
    private Long sharedBy;
    
    /** 分享类型: 1-链接分享, 2-指定用户, 3-群组分享 */
    private Integer shareType;
    
    /** 目标用户或群组ID列表 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> shareTargets;
    
    /** 允许下载 */
    private Boolean allowDownload;
    
    /** 允许预览 */
    private Boolean allowPreview;
    
    /** 允许保存 */
    private Boolean allowSave;
    
    /** 查看次数 */
    private Integer viewCount;
    
    /** 下载次数 */
    private Integer downloadCount;
    
    /** 保存次数 */
    private Integer saveCount;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    private LocalDateTime expiresAt;
    
    // 分享类型常量
    public static final int TYPE_LINK = 1;
    public static final int TYPE_USER = 2;
    public static final int TYPE_GROUP = 3;
}
