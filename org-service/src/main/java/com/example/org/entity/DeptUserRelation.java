package com.example.org.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 部门-用户关联实体
 */
@Data
@Accessors(chain = true)
@TableName("dept_user_relation")
public class DeptUserRelation {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 部门ID */
    private Long deptId;
    
    /** 用户ID */
    private Long userId;
    
    /** 是否主属部门 */
    private Boolean isPrimary;
    
    /** 职位 */
    private String title;
    
    /** 员工编号 */
    private String employeeNo;
    
    /** 加入时间 */
    private LocalDateTime joinedAt;
    
    private LocalDateTime createdAt;
}
