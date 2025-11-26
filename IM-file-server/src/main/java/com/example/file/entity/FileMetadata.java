package com.example.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物理文件元数据（CAS核心表）
 */
@Data
@TableName("file_metadata")
public class FileMetadata {

    /**
     * SHA-256哈希值作为主键
     */
    @TableId(type = IdType.INPUT)
    private String fileHash;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型：image, video, document, audio
     */
    private String fileType;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 扩展名
     */
    private String ext;

    /**
     * 存储后端：minio, s3, oss, local
     */
    private String storageBackend;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 对象键（CAS路径）
     */
    private String objectKey;

    /**
     * 引用计数（用于垃圾回收）
     */
    private Integer refCount;

    /**
     * 缩略图路径 (JSON)
     */
    private String thumbnailKeys;

    /**
     * 预览文件路径
     */
    private String previewKey;

    /**
     * 图片/视频宽度
     */
    private Integer width;

    /**
     * 图片/视频高度
     */
    private Integer height;

    /**
     * 音频/视频时长（秒）
     */
    private Integer duration;

    /**
     * 比特率
     */
    private Integer bitrate;

    /**
     * 页数（文档）
     */
    private Integer pageCount;

    /**
     * 字数（文档）
     */
    private Integer wordCount;

    /**
     * 病毒扫描状态：0-未检测, 1-安全, 2-病毒
     */
    private Integer virusScanStatus;

    /**
     * 病毒扫描结果
     */
    private String virusScanResult;

    /**
     * 内容审核状态：0-未审核, 1-通过, 2-违规
     */
    private Integer contentAuditStatus;

    /**
     * 内容审核结果 (JSON)
     */
    private String contentAuditResult;

    /**
     * 扩展信息 (JSON)
     */
    private String extra;

    /**
     * 状态：1-正常, 2-待删除, 3-已删除
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
