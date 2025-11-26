package com.example.domain.event;

import lombok.Data;

/**
 * 租户变更事件，对应表 sys_tenant
 */
@Data
public class TenantChangedEvent {

    /** 主键 ID */
    private Long id;

    /** 租户名称 */
    private String name;

    /** 租户编码（唯一） */
    private String code;

    /** 状态：1=正常，其它=禁用 */
    private Integer status;

    /** 是否已删除 */
    private Boolean deleted;

    /** 操作类型：UPSERT / DELETE */
    private String operation;
}
