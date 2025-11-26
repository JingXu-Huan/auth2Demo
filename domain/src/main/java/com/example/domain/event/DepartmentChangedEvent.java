package com.example.domain.event;

import lombok.Data;

/**
 * 部门变更事件，对应表 sys_department
 */
@Data
public class DepartmentChangedEvent {

    /** 部门主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 父部门 ID，0 表示根部门 */
    private Long parentId;

    /** 部门名称 */
    private String name;

    /** 排序 */
    private Integer orderNum;

    /** 部门负责人用户 ID */
    private Long leaderId;

    /** 状态：1=正常，其它=禁用 */
    private Integer status;

    /** 是否已删除 */
    private Boolean deleted;

    /** 操作类型：UPSERT / DELETE */
    private String operation;
}
