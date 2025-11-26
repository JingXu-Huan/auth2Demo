package com.example.domain.event;

import lombok.Data;

/**
 * 部门-用户关联变更事件，对应表 sys_dept_user_rel
 */
@Data
public class DeptUserRelChangedEvent {

    /** 关联主键 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 部门 ID */
    private Long departmentId;

    /** 用户 ID */
    private Long userId;

    /** 是否主部门 */
    private Boolean primary;

    /** 是否部门负责人（副本标记，可选使用） */
    private Boolean leader;

    /** 部门内排序 */
    private Integer orderNum;

    /** 操作类型：UPSERT / DELETE */
    private String operation;
}
