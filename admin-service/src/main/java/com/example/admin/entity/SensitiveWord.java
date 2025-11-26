package com.example.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 敏感词实体
 */
@Data
@Accessors(chain = true)
@TableName("sensitive_words")
public class SensitiveWord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 敏感词 */
    private String word;
    
    /** 分类: POLITICAL, PORN, AD, ABUSE */
    private String category;
    
    /** 处理方式: 1拦截 2替换*** 3报警 */
    private Integer actionType;
    
    /** 创建人ID */
    private Long createdBy;
    
    private LocalDateTime updatedAt;
    
    // 处理方式常量
    public static final int ACTION_BLOCK = 1;
    public static final int ACTION_REPLACE = 2;
    public static final int ACTION_ALERT = 3;
}
