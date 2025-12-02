package com.example.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * ====================================================================
 * 统一响应封装类 (Unified Response Wrapper)
 * ====================================================================
 * 
 * 【设计目的】
 * 为所有API接口提供统一的响应格式，方便前端处理：
 * - 统一的状态码判断逻辑
 * - 统一的错误处理方式
 * - 统一的数据解析方式
 * 
 * 【响应格式示例】
 * 成功响应：
 * {
 *   "code": 200,
 *   "message": "操作成功",
 *   "data": { ... },
 *   "timestamp": 1699999999999
 * }
 * 
 * 失败响应：
 * {
 *   "code": 500,
 *   "message": "用户名已存在",
 *   "data": null,
 *   "timestamp": 1699999999999
 * }
 * 
 * 【常用状态码】
 * - 200: 成功
 * - 400: 请求参数错误
 * - 401: 未认证（需要登录）
 * - 403: 无权限
 * - 404: 资源不存在
 * - 500: 服务器内部错误
 * 
 * 【泛型设计】
 * Result<T> 使用泛型，可以封装任意类型的数据：
 * - Result<User>: 返回单个用户
 * - Result<List<User>>: 返回用户列表
 * - Result<Void>: 无返回数据
 * 
 * 【使用示例】
 * Controller中：
 *   return Result.success(user);           // 成功，带数据
 *   return Result.success();               // 成功，无数据
 *   return Result.error("用户名已存在");    // 失败
 *   return Result.error(401, "请先登录");   // 失败，带状态码
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data  // Lombok: 自动生成getter/setter/toString/equals/hashCode
public class Result<T> implements Serializable {
    
    /** 序列化版本号 - 用于反序列化时的版本兼容 */
    private static final long serialVersionUID = 1L;
    
    /**
     * 响应状态码
     * 200表示成功，其他表示失败
     * 遵循HTTP状态码规范
     */
    private Integer code;
    
    /**
     * 响应消息
     * 成功时通常为"操作成功"
     * 失败时为具体的错误信息
     */
    private String message;
    
    /**
     * 响应数据
     * 使用泛型，可以是任意类型
     * 失败时通常为null
     */
    private T data;
    
    /**
     * 响应时间戳
     * 记录响应生成的时间
     * 用于调试和日志追踪
     */
    private Long timestamp;
    
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }
    
    /**
     * 成功响应（带消息）
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null);
    }
    
    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }
    
    /**
     * 成功响应（带消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    /**
     * 失败响应（带消息）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
    
    /**
     * 失败响应（带错误码和消息）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    
    /**
     * 失败响应（带错误码、消息和数据）
     */
    public static <T> Result<T> error(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code != null && this.code == 200;
    }
}
