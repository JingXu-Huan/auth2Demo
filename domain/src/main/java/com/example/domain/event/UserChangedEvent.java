package com.example.domain.event;

import lombok.Data;

/**
 * 用户变更事件，对应表 sys_user
 */
@Data
public class UserChangedEvent {

    /** 用户主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    private String username;

    private String realName;

    private String email;

    private String mobile;

    private String avatarUrl;

    /** 直属上级用户 ID */
    private Long directLeaderId;

    /** 状态：1=正常，其它=禁用 */
    private Integer status;

    /** 是否已删除 */
    private Boolean deleted;

    /** 操作类型：UPSERT / DELETE */
    private String operation;
}
