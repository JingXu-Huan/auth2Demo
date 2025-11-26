package com.example.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件分片上传会话
 */
@Data
@TableName("file_upload_sessions")
public class FileUploadSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 上传会话ID
     */
    private String uploadId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 文件哈希（用于秒传）
     */
    private String fileHash;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 每个分片大小
     */
    private Integer chunkSize;

    /**
     * 总分片数
     */
    private Integer chunkCount;

    /**
     * 已上传分片数
     */
    private Integer uploadedChunks;

    /**
     * 各分片的ETag (JSON)
     */
    private String chunkEtags;

    /**
     * MinIO的uploadId
     */
    private String minioUploadId;

    /**
     * 状态：1-上传中, 2-合并中, 3-完成, 4-失败, 5-已取消
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
}
