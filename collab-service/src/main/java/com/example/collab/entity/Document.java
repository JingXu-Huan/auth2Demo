package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 文档实体
 */
@Data
@TableName("documents")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文档唯一标识
     */
    private String docId;

    /**
     * 标题
     */
    private String title;

    /**
     * 文档类型：text, spreadsheet, slide, canvas
     */
    private String docType;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 所有者ID
     */
    private Long ownerId;

    /**
     * 团队ID
     */
    private Long teamId;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 文件夹ID
     */
    private Long folderId;

    /**
     * 内容版本号
     */
    private Long contentVersion;

    /**
     * 内容大小
     */
    private Long contentSize;

    /**
     * 存储类型：database, minio
     */
    private String storageType;

    /**
     * 小文档直接存储
     */
    private byte[] content;

    /**
     * 大文档MinIO URL
     */
    private String contentUrl;

    /**
     * Yjs文档状态
     */
    private byte[] yjsState;

    /**
     * 向量时钟 (JSON)
     */
    private String vectorClock;

    /**
     * 协同锁用户ID
     */
    private Long lockUserId;

    /**
     * 锁Token
     */
    private String lockToken;

    /**
     * 锁过期时间
     */
    private OffsetDateTime lockExpiresAt;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 编辑次数
     */
    private Integer editCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 收藏数
     */
    private Integer starCount;

    /**
     * 可见性：private, team, public
     */
    private String visibility;

    /**
     * 允许评论
     */
    private Boolean allowComment;

    /**
     * 允许复制
     */
    private Boolean allowCopy;

    /**
     * 允许下载
     */
    private Boolean allowDownload;

    /**
     * 自定义URL
     */
    private String slug;

    /**
     * 封面图片
     */
    private String coverImage;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 状态：1-正常, 2-归档, 3-回收站, 4-已删除
     */
    private Integer status;

    /**
     * 是否已发布
     */
    private Boolean published;

    private OffsetDateTime publishedAt;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;
}
