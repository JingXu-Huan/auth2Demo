package com.example.common.exception;

import com.example.domain.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import java.util.stream.Collectors;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 全局异常处理器
 * 统一处理各种异常，返回标准的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 处理参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数校验失败: {}", errors);
        
        return ResponseEntity
                .badRequest()
                .body(Result.error(400, "参数校验失败: " + errors));
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数绑定失败: {}", errors);
        
        return ResponseEntity
                .badRequest()
                .body(Result.error(400, "参数绑定失败: " + errors));
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
        
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(Result.error(ex.getCode(), ex.getMessage()));
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());
        
        return ResponseEntity
                .badRequest()
                .body(Result.error(400, "非法参数: " + ex.getMessage()));
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Result<Void>> handleNullPointerException(NullPointerException ex) {
        log.error("空指针异常", ex);
        
        // 生产环境不应该暴露详细错误信息
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "服务器内部错误，请联系管理员"));
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-17
     * 处理实体未找到异常（JPA）
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Result<Void>> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("实体未找到: {}", ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Result.error(404, ex.getMessage()));
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-17
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("运行时异常: {}", ex.getMessage(), ex);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, ex.getMessage()));
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("未知异常", ex);
        
        // 生产环境不应该暴露详细错误信息，这里仅用于开发调试
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "服务器内部错误，请稍后重试"));
    }
}
