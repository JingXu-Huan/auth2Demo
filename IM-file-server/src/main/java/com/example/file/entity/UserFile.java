package com.example.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户文件关联表
 */
@Data
@TableName("user_files")
public class UserFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 文件哈希
     */
    private String fileHash;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 用户视角的虚拟路径
     */
    private String filePath;

    /**
     * 所属文件夹ID
     */
    private Long folderId;

    /**
     * 来源类型：message, document, avatar, upload
     */
    private String sourceType;

    /**
     * 来源ID
     */
    private String sourceId;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 分享链接Token
     */
    private String shareToken;

    /**
     * 分享密码
     */
    private String sharePassword;

    /**
     * 分享过期时间
     */
    private LocalDateTime shareExpiresAt;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 标签 (JSON数组)
     */
    private String tags;

    /**
     * 分类
     */
    private String category;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 软删除时间
     */
    private LocalDateTime deletedAt;
}
