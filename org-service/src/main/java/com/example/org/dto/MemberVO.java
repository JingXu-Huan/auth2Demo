package com.example.org.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成员信息VO
 */
@Data
public class MemberVO {
    
    private Long userId;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    
    /** 部门ID */
    private Long deptId;
    /** 部门名称 */
    private String deptName;
    /** 部门路径名称 (如: 总部/研发部/后端组) */
    private String deptPathName;
    
    /** 是否主属部门 */
    private Boolean isPrimary;
    /** 职位 */
    private String title;
    /** 员工编号 */
    private String employeeNo;
    
    /** 拼音 (用于搜索) */
    private String pinyin;
    /** 拼音首字母 */
    private String pinyinInitial;
    
    /** 加入时间 */
    private LocalDateTime joinedAt;
}
