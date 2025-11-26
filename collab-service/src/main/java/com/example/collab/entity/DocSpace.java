package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 文档空间实体
 * 对应 doc_db.doc_spaces 表
 */
@Data
@Accessors(chain = true)
@TableName("doc_spaces")
public class DocSpace {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 空间名称 */
    private String name;

    /** 空间描述 */
    private String description;

    /** 图标 */
    private String icon;

    /** 所有者ID */
    private Long ownerId;

    /** 团队ID */
    private Long teamId;

    /** 是否公开 */
    private Boolean isPublic;

    /** 是否允许访客 */
    private Boolean allowGuest;

    /** 文档数量 */
    private Integer docCount;

    /** 成员数量 */
    private Integer memberCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
