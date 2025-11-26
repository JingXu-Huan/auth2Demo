package com.example.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 文件夹实体
 * 对应 file_db.folders 表
 */
@Data
@Accessors(chain = true)
@TableName("folders")
public class Folder {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long parentId;
    
    /** 文件夹名称 */
    private String name;
    
    /** 完整路径 */
    private String path;
    
    /** 层级 */
    private Integer level;
    
    /** 文件数 */
    private Integer fileCount;
    
    /** 子文件夹数 */
    private Integer folderCount;
    
    /** 总大小 */
    private Long totalSize;
    
    /** 是否共享 */
    private Boolean isShared;
    
    /** 文件夹颜色 */
    private String color;
    
    /** 图标 */
    private String icon;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /** 软删除时间 */
    private LocalDateTime deletedAt;
}
