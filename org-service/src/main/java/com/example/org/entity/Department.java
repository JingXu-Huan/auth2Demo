package com.example.org.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门实体
 * 对应 org_db.departments 表
 */
@Data
@Accessors(chain = true)
@TableName(value = "departments", autoResultMap = true)
public class Department {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 组织ID */
    private Long orgId;
    
    /** 父部门ID */
    private Long parentId;
    
    /** 部门编码 */
    private String deptCode;
    
    /** 部门名称 */
    private String name;
    
    /** 完整名称 */
    private String fullName;
    
    /** 部门路径: /1/2/3 */
    private String path;
    
    /** 路径名称: 总部/研发部/后端组 */
    private String pathNames;
    
    /** 层级 */
    private Integer level;
    
    /** 排序 */
    private Integer sortOrder;
    
    /** 部门负责人ID */
    private Long managerId;
    
    /** 副职领导IDs */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> deputyIds;
    
    /** 成员数量 */
    private Integer memberCount;
    
    /** 子部门数量 */
    private Integer subDeptCount;
    
    /** 预算 */
    private BigDecimal budget;
    
    /** 成本中心 */
    private String costCenter;
    
    /** 状态: 1正常 2撤销 */
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    // 状态常量
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_DISBANDED = 2;
}
