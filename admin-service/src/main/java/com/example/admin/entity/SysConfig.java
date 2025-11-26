package com.example.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 系统配置实体
 */
@Data
@Accessors(chain = true)
@TableName("sys_configs")
public class SysConfig {
    
    @TableId
    private String configKey;
    
    /** 配置值 */
    private String configValue;
    
    /** 描述 */
    private String description;
    
    /** 更新人 */
    private Long updatedBy;
    
    private LocalDateTime updatedAt;
}
