package com.example.org.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 组织成员实体
 * 对应 org_db.org_members 表
 */
@Data
@Accessors(chain = true)
@TableName("org_members")
public class OrgMember {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 组织ID */
    private Long orgId;
    
    /** 用户ID */
    private Long userId;
    
    /** 部门ID */
    private Long deptId;
    
    /** 员工编号 */
    private String employeeNo;
    
    /** 工作邮箱 */
    private String workEmail;
    
    /** 工作电话 */
    private String workPhone;
    
    /** 职位 */
    private String jobTitle;
    
    /** 职级 */
    private String jobLevel;
    
    /** 序列（技术/管理/专业）*/
    private String jobSequence;
    
    /** 入职日期 */
    private LocalDate hireDate;
    
    /** 试用期结束日期 */
    private LocalDate probationEndDate;
    
    /** 合同结束日期 */
    private LocalDate contractEndDate;
    
    /** 直属上级ID */
    private Long directManagerId;
    
    /** 虚线上级ID */
    private Long dottedManagerId;
    
    /** 办公地点 */
    private String officeLocation;
    
    /** 工位号 */
    private String seatNumber;
    
    /** 状态: 1在职 2离职 3休假 */
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /** 离职时间 */
    private LocalDateTime leftAt;
    
    // 状态常量
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_LEFT = 2;
    public static final int STATUS_ON_LEAVE = 3;
}
