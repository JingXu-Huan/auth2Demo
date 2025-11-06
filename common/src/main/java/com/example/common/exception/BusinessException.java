package com.example.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况
 * 
 * @author Security Team
 * @version 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {
    
    /**
     * 错误码
     */
    private final int code;
    
    /**
     * HTTP 状态码
     */
    private final HttpStatus httpStatus;
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误信息
     * @param httpStatus HTTP状态码
     */
    public BusinessException(int code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误信息
     * @param cause 原因
     */
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
    
    /**
     * 常用业务异常
     */
    public static class UserNotFoundException extends BusinessException {
        public UserNotFoundException(String message) {
            super(404, message, HttpStatus.NOT_FOUND);
        }
    }
    
    public static class UserAlreadyExistsException extends BusinessException {
        public UserAlreadyExistsException(String message) {
            super(409, message, HttpStatus.CONFLICT);
        }
    }
    
    public static class UnauthorizedException extends BusinessException {
        public UnauthorizedException(String message) {
            super(401, message, HttpStatus.UNAUTHORIZED);
        }
    }
    
    public static class ForbiddenException extends BusinessException {
        public ForbiddenException(String message) {
            super(403, message, HttpStatus.FORBIDDEN);
        }
    }
}
