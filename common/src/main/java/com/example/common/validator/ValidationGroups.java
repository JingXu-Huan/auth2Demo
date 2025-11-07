package com.example.common.validator;

/**
 * 验证分组接口
 * 用于区分不同场景的验证规则
 * 
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 */
public class ValidationGroups {
    
    /**
     * 创建操作验证组
     */
    public interface Create {}
    
    /**
     * 更新操作验证组
     */
    public interface Update {}
    
    /**
     * 删除操作验证组
     */
    public interface Delete {}
}
