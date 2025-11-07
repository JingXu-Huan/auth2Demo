package com.example.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 分页结果 VO
 * 用于返回分页数据
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data
/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 */
public class PageResult<T> {
    
    /**
     * 当前页码
     */
    private Long current;
    
    /**
     * 每页大小
     */
    private Long size;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 总页数
     */
    private Long pages;
    
    /**
     * 数据列表
     */
    private List<T> records;
    
    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return current > 1;
    }
    
    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return current < pages;
    }
}
