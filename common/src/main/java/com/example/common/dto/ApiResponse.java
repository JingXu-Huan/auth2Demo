package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 统一的 API 响应格式（共享模块）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    
    private boolean success;
    private String message;
    private Object data;
    private Long timestamp;
    
    public static ApiResponse success(String message) {
        return new ApiResponse(true, message, null, System.currentTimeMillis());
    }
    
    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(true, message, data, System.currentTimeMillis());
    }
    
    public static ApiResponse error(String message) {
        return new ApiResponse(false, message, null, System.currentTimeMillis());
    }
    
    public static ApiResponse error(String message, Object data) {
        return new ApiResponse(false, message, data, System.currentTimeMillis());
    }
}
