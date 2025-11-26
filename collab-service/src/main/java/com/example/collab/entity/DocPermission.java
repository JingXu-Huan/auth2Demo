package com.example.collab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 文档权限实体
 * 对应 doc_db.doc_permissions 表
 */
@Data
@Accessors(chain = true)
@TableName("doc_permissions")
public class DocPermission {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long docId;
    
    /** 权限主体类型: user, team, role, public */
    private String granteeType;
    
    /** 权限主体ID */
    private Long granteeId;
    
    /** 权限级别: view, comment, edit, admin */
    private String permissionLevel;
    
    private Boolean canView;
    
    private Boolean canComment;
    
    private Boolean canEdit;
    
    private Boolean canDelete;
    
    private Boolean canShare;
    
    /** 过期时间 */
    private LocalDateTime expiresAt;
    
    /** 授权人ID */
    private Long grantedBy;
    
    private LocalDateTime grantedAt;
    
    // 权限级别常量
    public static final String LEVEL_VIEW = "view";
    public static final String LEVEL_COMMENT = "comment";
    public static final String LEVEL_EDIT = "edit";
    public static final String LEVEL_ADMIN = "admin";
    
    // 主体类型常量
    public static final String GRANTEE_USER = "user";
    public static final String GRANTEE_TEAM = "team";
    public static final String GRANTEE_ROLE = "role";
    public static final String GRANTEE_PUBLIC = "public";
}
