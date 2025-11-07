package com.example.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 响应 DTO
 * 用于统一的 API 响应格式
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 */
public class ApiResponse<T> {
    
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "success", null);
    }
    
    /**
     * 成功响应（带消息，无数据）
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(200, message, null);
    }
    
    /**
     * 成功响应（带数据和消息）
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(200, message, data);
    }
    
    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }
    
    /**
     * 失败响应（带错误码）
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
